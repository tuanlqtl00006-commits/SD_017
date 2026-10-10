import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { API_DIA_CHI, diaChiHanhChinhService } from '../src/services/diaChiHanhChinhService'

vi.mock('axios', () => ({ default: { get: vi.fn() } }))

const TINH = [
  { name: 'Tỉnh Hưng Yên', code: 33, division_type: 'tỉnh' },
  { name: 'Thành phố Hà Nội', code: 1, division_type: 'thành phố trung ương' },
]

describe('API địa chỉ sau sáp nhập (provinces.open-api.vn v2)', () => {
  beforeEach(() => {
    diaChiHanhChinhService.xoaNho()
    axios.get.mockReset()
  })

  it('dùng đúng phiên bản 2 của API', () => {
    expect(API_DIA_CHI).toBe('https://provinces.open-api.vn/api/v2')
  })

  it('lấy danh sách tỉnh/thành, sắp xếp theo tên và nhớ lại (chỉ gọi 1 lần)', async () => {
    axios.get.mockResolvedValue({ data: TINH })
    const a = await diaChiHanhChinhService.layTinhThanh()
    const b = await diaChiHanhChinhService.layTinhThanh()
    expect(a.map((t) => t.name)).toEqual(['Thành phố Hà Nội', 'Tỉnh Hưng Yên'])
    expect(b).toBe(a)
    expect(axios.get).toHaveBeenCalledTimes(1)
    expect(axios.get.mock.calls[0][0]).toBe('https://provinces.open-api.vn/api/v2/p/')
  })

  it('lấy phường/xã của một tỉnh bằng depth=2, không có cấp quận/huyện', async () => {
    axios.get.mockResolvedValue({ data: { name: 'Thành phố Hà Nội', code: 1, wards: [{ name: 'Phường Hồng Hà', code: 4 }, { name: 'Phường Ba Đình', code: 1 }] } })
    const ds = await diaChiHanhChinhService.layPhuongXa(1)
    expect(ds.map((w) => w.name)).toEqual(['Phường Ba Đình', 'Phường Hồng Hà'])
    expect(axios.get.mock.calls[0][0]).toBe('https://provinces.open-api.vn/api/v2/p/1')
    expect(axios.get.mock.calls[0][1].params).toEqual({ depth: 2 })
    await diaChiHanhChinhService.layPhuongXa(1)
    expect(axios.get).toHaveBeenCalledTimes(1) // nhớ lại
  })

  it('đường dẫn chính lỗi thì thử đường dẫn dự phòng', async () => {
    axios.get.mockRejectedValueOnce(new Error('404')).mockResolvedValueOnce({ data: TINH })
    const ds = await diaChiHanhChinhService.layTinhThanh()
    expect(ds).toHaveLength(2)
    expect(axios.get.mock.calls[1][0]).toBe('https://provinces.open-api.vn/api/v2/')
  })

  it('dữ liệu sai định dạng thì báo lỗi', async () => {
    axios.get.mockResolvedValue({ data: { loi: true } })
    await expect(diaChiHanhChinhService.layTinhThanh()).rejects.toThrow(/không đúng định dạng/)
  })

  it('không có mã tỉnh thì trả về danh sách rỗng, không gọi API', async () => {
    expect(await diaChiHanhChinhService.layPhuongXa('')).toEqual([])
    expect(axios.get).not.toHaveBeenCalled()
  })
})
