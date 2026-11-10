// Xuất file .xlsx thật, không cần thư viện ngoài (tự đóng gói zip dạng "store").

const XML_HEAD = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
const MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

const escapeXml = (value) =>
  String(value)
    // eslint-disable-next-line no-control-regex
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

/** 0 -> 'A', 25 -> 'Z', 26 -> 'AA' */
function columnName(index) {
  let n = index + 1
  let name = ''
  while (n > 0) {
    const rem = (n - 1) % 26
    name = String.fromCharCode(65 + rem) + name
    n = Math.floor((n - 1) / 26)
  }
  return name
}

function sheetXml(columns, rows) {
  const lastCell = `${columnName(columns.length - 1)}${rows.length + 1}`
  // Cột văn bản (text: true) có định dạng "@" cho CẢ cột: ô còn trống cũng là văn bản, nên người dùng gõ 0912345678 hay 25/12/2000
  // vào file mẫu thì Excel giữ nguyên chữ số 0 đầu và không tự đổi sang ngày theo kiểu tháng/ngày của máy.
  const cols = columns
    .map((c, i) => `<col min="${i + 1}" max="${i + 1}" width="${c.width || 16}" customWidth="1"${c.text ? ' style="3"' : ''}/>`)
    .join('')

  const header = columns
    .map((c, i) => `<c r="${columnName(i)}1" s="1" t="inlineStr"><is><t xml:space="preserve">${escapeXml(c.header)}</t></is></c>`)
    .join('')

  const body = rows
    .map((row, r) => {
      const cells = columns
        .map((c, i) => {
          const value = row[c.key]
          if (value === null || value === undefined || value === '') return ''
          const ref = `${columnName(i)}${r + 2}`
          // Cột tiền (money: true) dùng định dạng số có ngăn cách hàng nghìn (#,##0) nhưng vẫn là SỐ để Excel cộng được
          return typeof value === 'number'
            ? `<c r="${ref}"${c.money ? ' s="2"' : ''}><v>${value}</v></c>`
            : `<c r="${ref}"${c.text ? ' s="3"' : ''} t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>`
        })
        .join('')
      return `<row r="${r + 2}">${cells}</row>`
    })
    .join('')

  return (
    XML_HEAD +
    '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
    `<dimension ref="A1:${lastCell}"/>` +
    '<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/><selection pane="bottomLeft"/></sheetView></sheetViews>' +
    `<cols>${cols}</cols>` +
    `<sheetData><row r="1">${header}</row>${body}</sheetData>` +
    '</worksheet>'
  )
}

const STYLES_XML =
  XML_HEAD +
  '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
  '<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>' +
  '<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>' +
  '<fill><patternFill patternType="solid"><fgColor rgb="FFE7F0FF"/><bgColor indexed="64"/></patternFill></fill></fills>' +
  '<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>' +
  '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>' +
  '<cellXfs count="4"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>' +
  '<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>' +
  '<xf numFmtId="3" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>' +
  '<xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs>' +
  '<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>' +
  '</styleSheet>'

/**
 * Bảng định dạng chuyên nghiệp cho báo cáo thống kê (có tiêu đề lớn, thông tin kỳ lọc,
 * bảng kẻ khung đầy đủ, dòng xen kẽ, định dạng tiền tệ #,##0 ₫ và dòng Tổng cộng in đậm).
 */
const REPORT_STYLES_XML =
  XML_HEAD +
  '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
  '<numFmts count="2">' +
  '<numFmt numFmtId="164" formatCode="#,##0&quot; ₫&quot;"/>' +
  '<numFmt numFmtId="165" formatCode="#,##0"/>' +
  '</numFmts>' +
  '<fonts count="7">' +
  '<font><sz val="11"/><color rgb="FF1E293B"/><name val="Segoe UI"/></font>' +
  '<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Segoe UI"/></font>' +
  '<font><b/><sz val="10"/><color rgb="FF1D4ED8"/><name val="Segoe UI"/></font>' +
  '<font><b/><sz val="14"/><color rgb="FF0F172A"/><name val="Segoe UI"/></font>' +
  '<font><i/><sz val="10"/><color rgb="FF475569"/><name val="Segoe UI"/></font>' +
  '<font><b/><sz val="11"/><color rgb="FF0F172A"/><name val="Segoe UI"/></font>' +
  '<font><b/><sz val="11"/><color rgb="FF1D4ED8"/><name val="Segoe UI"/></font>' +
  '</fonts>' +
  '<fills count="6">' +
  '<fill><patternFill patternType="none"/></fill>' +
  '<fill><patternFill patternType="gray125"/></fill>' +
  '<fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/><bgColor indexed="64"/></patternFill></fill>' +
  '<fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor indexed="64"/></patternFill></fill>' +
  '<fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/><bgColor indexed="64"/></patternFill></fill>' +
  '<fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/><bgColor indexed="64"/></patternFill></fill>' +
  '</fills>' +
  '<borders count="4">' +
  '<border><left/><right/><top/><bottom/><diagonal/></border>' +
  '<border>' +
  '<left style="thin"><color rgb="FFCBD5E1"/></left>' +
  '<right style="thin"><color rgb="FFCBD5E1"/></right>' +
  '<top style="thin"><color rgb="FFCBD5E1"/></top>' +
  '<bottom style="thin"><color rgb="FFCBD5E1"/></bottom>' +
  '<diagonal/>' +
  '</border>' +
  '<border>' +
  '<left style="thin"><color rgb="FF3B82F6"/></left>' +
  '<right style="thin"><color rgb="FF3B82F6"/></right>' +
  '<top style="thin"><color rgb="FF1E3A8A"/></top>' +
  '<bottom style="medium"><color rgb="FF1E3A8A"/></bottom>' +
  '<diagonal/>' +
  '</border>' +
  '<border>' +
  '<left style="thin"><color rgb="FFCBD5E1"/></left>' +
  '<right style="thin"><color rgb="FFCBD5E1"/></right>' +
  '<top style="thin"><color rgb="FF64748B"/></top>' +
  '<bottom style="double"><color rgb="FF1E3A8A"/></bottom>' +
  '<diagonal/>' +
  '</border>' +
  '</borders>' +
  '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>' +
  '<cellXfs count="20">' +
  /* 0: mặc định */
  '<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>' +
  /* 1..3: tiêu đề cột bảng (trái / giữa / phải) */
  '<xf numFmtId="0" fontId="1" fillId="2" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>' +
  '<xf numFmtId="0" fontId="1" fillId="2" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>' +
  '<xf numFmtId="0" fontId="1" fillId="2" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center" wrapText="1"/></xf>' +
  /* 4..6: dòng thương hiệu, tiêu đề lớn, phụ đề */
  '<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>' +
  '<xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>' +
  '<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>' +
  /* 7..10: dòng dữ liệu lẻ (trái / giữa / số nguyên phải / tiền tệ phải) */
  '<xf numFmtId="49" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>' +
  '<xf numFmtId="49" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>' +
  '<xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>' +
  '<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>' +
  /* 11..14: dòng dữ liệu chẵn nền xám nhạt (trái / giữa / số nguyên phải / tiền tệ phải) */
  '<xf numFmtId="49" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>' +
  '<xf numFmtId="49" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>' +
  '<xf numFmtId="165" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>' +
  '<xf numFmtId="164" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>' +
  /* 15..18: dòng TỔNG CỘNG (nhãn trái / nhãn giữa / số nguyên phải / tiền tệ phải) */
  '<xf numFmtId="49" fontId="5" fillId="4" borderId="3" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>' +
  '<xf numFmtId="49" fontId="5" fillId="4" borderId="3" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>' +
  '<xf numFmtId="165" fontId="5" fillId="4" borderId="3" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>' +
  '<xf numFmtId="164" fontId="6" fillId="4" borderId="3" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>' +
  /* 19: dòng tiêu đề nhóm con (section header) */
  '<xf numFmtId="49" fontId="5" fillId="5" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>' +
  '</cellXfs>' +
  '<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>' +
  '</styleSheet>'

function mergedBannerRow(rowNum, height, styleIdx, colCount, text) {
  const cells = Array.from({ length: colCount }, (_, i) => {
    const ref = `${columnName(i)}${rowNum}`
    if (i === 0 && text !== undefined && text !== null && text !== '') {
      return `<c r="${ref}" s="${styleIdx}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(text)}</t></is></c>`
    }
    return `<c r="${ref}" s="${styleIdx}"/>`
  }).join('')
  return `<row r="${rowNum}" ht="${height}" customHeight="1">${cells}</row>`
}

function reportSheetXml(sh, report = {}) {
  const columns = Array.isArray(sh.columns) && sh.columns.length ? sh.columns : [{ header: 'Nội dung', key: 'text', width: 60 }]
  const colCount = columns.length
  const lastCol = columnName(colCount - 1)
  const brand = sh.brand || report.brand || 'CỬA HÀNG VỢT CẦU LÔNG FOOTSTYLE — BÁO CÁO QUẢN TRỊ'
  const title = sh.title || report.title || sh.sheetName || 'BÁO CÁO THỐNG KÊ'
  const subtitle = sh.subtitle || report.subtitle || ''

  const colsXml = columns
    .map((c, i) => `<col min="${i + 1}" max="${i + 1}" width="${c.width || 18}" customWidth="1"/>`)
    .join('')

  const merges = [
    `A1:${lastCol}1`,
    `A2:${lastCol}2`,
    `A3:${lastCol}3`,
  ]

  const xmlRows = [
    mergedBannerRow(1, 20, 4, colCount, brand),
    mergedBannerRow(2, 26, 5, colCount, title),
    mergedBannerRow(3, 19, 6, colCount, subtitle),
    '<row r="4" ht="10" customHeight="1"/>',
  ]

  // Dòng 5: tiêu đề cột bảng
  const headerCells = columns
    .map((c, i) => {
      const st = c.align === 'center' ? 2 : c.money || c.number || c.align === 'right' ? 3 : 1
      return `<c r="${columnName(i)}5" s="${st}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(c.header)}</t></is></c>`
    })
    .join('')
  xmlRows.push(`<row r="5" ht="26" customHeight="1">${headerCells}</row>`)

  // Danh sách dòng dữ liệu + dòng tổng cộng + ghi chú/nhận xét
  const rawRows = Array.isArray(sh.rows) ? [...sh.rows] : []
  if (!rawRows.length) {
    rawRows.push({
      _isEmpty: true,
      text: sh.emptyText || 'Chưa có dữ liệu phát sinh trong kỳ báo cáo này.',
    })
  }
  if (sh.totalRow) {
    rawRows.push({ ...sh.totalRow, _isTotal: true })
  }
  if (Array.isArray(sh.notes) && sh.notes.length) {
    rawRows.push({ _isSpacer: true })
    rawRows.push({ _isSection: true, title: sh.notesTitle || 'NHẬN XÉT & PHÂN TÍCH TỔNG HỢP' })
    for (const line of sh.notes) {
      if (line) rawRows.push({ _isNote: true, text: line })
    }
  }

  let rowNum = 5
  let dataIdx = 0
  for (const row of rawRows) {
    rowNum += 1
    if (row._isSpacer) {
      xmlRows.push(`<row r="${rowNum}" ht="10" customHeight="1"/>`)
      continue
    }
    if (row._isSection) {
      if (colCount > 1) merges.push(`A${rowNum}:${lastCol}${rowNum}`)
      xmlRows.push(mergedBannerRow(rowNum, 23, 19, colCount, row.title || row[columns[0].key] || ''))
      dataIdx = 0
      continue
    }
    if (row._isEmpty || row._isNote) {
      if (colCount > 1) merges.push(`A${rowNum}:${lastCol}${rowNum}`)
      xmlRows.push(mergedBannerRow(rowNum, 21, 7, colCount, row.text || row[columns[0].key] || ''))
      continue
    }
    if (row._isTotal) {
      const cells = columns
        .map((c, i) => {
          const ref = `${columnName(i)}${rowNum}`
          const val = row[c.key]
          if (typeof val === 'number' && !Number.isNaN(val)) {
            const st = c.money ? 18 : 17
            return `<c r="${ref}" s="${st}"><v>${val}</v></c>`
          }
          const st = c.align === 'center' ? 16 : c.align === 'right' || c.money || c.number ? 17 : 15
          if (val === null || val === undefined || val === '') return `<c r="${ref}" s="${st}"/>`
          return `<c r="${ref}" s="${st}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(val)}</t></is></c>`
        })
        .join('')
      xmlRows.push(`<row r="${rowNum}" ht="24" customHeight="1">${cells}</row>`)
      continue
    }

    const even = dataIdx % 2 === 1
    dataIdx += 1
    const cells = columns
      .map((c, i) => {
        const ref = `${columnName(i)}${rowNum}`
        const val = row[c.key]
        const isMoney = Boolean(c.money || row._moneyKeys?.includes(c.key))
        const isNumber = Boolean(c.number || typeof val === 'number')
        if (typeof val === 'number' && !Number.isNaN(val)) {
          const st = isMoney ? (even ? 14 : 10) : c.align === 'center' ? (even ? 12 : 8) : (even ? 13 : 9)
          return `<c r="${ref}" s="${st}"><v>${val}</v></c>`
        }
        const st =
          c.align === 'center'
            ? even ? 12 : 8
            : c.align === 'right' || isMoney || isNumber
              ? even ? 13 : 9
              : even ? 11 : 7
        if (val === null || val === undefined || val === '') return `<c r="${ref}" s="${st}"/>`
        return `<c r="${ref}" s="${st}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(val)}</t></is></c>`
      })
      .join('')
    xmlRows.push(`<row r="${rowNum}" ht="21" customHeight="1">${cells}</row>`)
  }

  const lastCell = `${lastCol}${rowNum}`
  const mergeXml =
    colCount > 1 && merges.length
      ? `<mergeCells count="${merges.length}">${merges.map((m) => `<mergeCell ref="${m}"/>`).join('')}</mergeCells>`
      : ''

  return (
    XML_HEAD +
    '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
    `<dimension ref="A1:${lastCell}"/>` +
    '<sheetViews><sheetView workbookViewId="0"><pane ySplit="5" topLeftCell="A6" activePane="bottomLeft" state="frozen"/><selection pane="bottomLeft" activeCell="A6" sqref="A6"/></sheetView></sheetViews>' +
    '<sheetFormatPr defaultRowHeight="20"/>' +
    `<cols>${colsXml}</cols>` +
    `<sheetData>${xmlRows.join('')}</sheetData>` +
    mergeXml +
    '</worksheet>'
  )
}

const crcTable = (() => {
  const table = new Uint32Array(256)
  for (let n = 0; n < 256; n++) {
    let c = n
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    table[n] = c >>> 0
  }
  return table
})()

function crc32(bytes) {
  let c = 0xffffffff
  for (let i = 0; i < bytes.length; i++) c = crcTable[(c ^ bytes[i]) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}

/** Gói các file vào một zip không nén. files: [{ name, text }] */
function zipStore(files) {
  const enc = new TextEncoder()
  const localParts = []
  const centralParts = []
  let offset = 0

  for (const file of files) {
    const name = enc.encode(file.name)
    const data = enc.encode(file.text)
    const crc = crc32(data)

    const local = new Uint8Array(30 + name.length)
    const lv = new DataView(local.buffer)
    lv.setUint32(0, 0x04034b50, true)
    lv.setUint16(4, 20, true)
    lv.setUint16(6, 0x0800, true) // tên file UTF-8
    lv.setUint16(8, 0, true) // không nén
    lv.setUint16(10, 0, true)
    lv.setUint16(12, 0x21, true) // ngày 1980-01-01
    lv.setUint32(14, crc, true)
    lv.setUint32(18, data.length, true)
    lv.setUint32(22, data.length, true)
    lv.setUint16(26, name.length, true)
    local.set(name, 30)

    const central = new Uint8Array(46 + name.length)
    const cv = new DataView(central.buffer)
    cv.setUint32(0, 0x02014b50, true)
    cv.setUint16(4, 20, true)
    cv.setUint16(6, 20, true)
    cv.setUint16(8, 0x0800, true)
    cv.setUint16(14, 0x21, true)
    cv.setUint32(16, crc, true)
    cv.setUint32(20, data.length, true)
    cv.setUint32(24, data.length, true)
    cv.setUint16(28, name.length, true)
    cv.setUint32(42, offset, true)
    central.set(name, 46)

    localParts.push(local, data)
    centralParts.push(central)
    offset += local.length + data.length
  }

  const centralSize = centralParts.reduce((sum, part) => sum + part.length, 0)
  const end = new Uint8Array(22)
  const ev = new DataView(end.buffer)
  ev.setUint32(0, 0x06054b50, true)
  ev.setUint16(8, files.length, true)
  ev.setUint16(10, files.length, true)
  ev.setUint32(12, centralSize, true)
  ev.setUint32(16, offset, true)

  return new Blob([...localParts, ...centralParts, end], { type: MIME })
}

/**
 * Tạo Blob .xlsx.
 * columns: [{ header, key, width, money, text }]  rows: [{ [key]: string | number }]
 *   money: true = cột tiền, hiện 3.861.000 (vẫn là SỐ để Excel cộng được)
 *   text : true = cả cột là văn bản (định dạng "@"): giữ số 0 đầu của số điện thoại / căn cước và ngày dd/mm/yyyy dạng chữ
 */
export function buildXlsx({ sheetName = 'Sheet1', columns, rows }) {
  return buildXlsxNhieuSheet([{ sheetName, columns, rows }])
}

/** Tạo Blob .xlsx nhiều sheet: sheets = [{ sheetName, columns, rows }] (vd báo cáo thống kê: mỗi bảng một sheet). */
export function buildXlsxNhieuSheet(sheets, report = null) {
  const isReport = Boolean(
    report || sheets.some((sh) => sh.title || sh.subtitle || sh.totalRow || sh.notes),
  )
  const daDung = new Set()
  const ten = sheets.map((sh, i) => {
    let t = String(sh.sheetName || `Sheet${i + 1}`).replace(/[[\]:*?/\\]/g, ' ').slice(0, 31) || `Sheet${i + 1}`
    while (daDung.has(t.toLowerCase())) t = `${t.slice(0, 27)} ${i + 1}`
    daDung.add(t.toLowerCase())
    return t
  })
  const so = sheets.map((_, i) => i + 1)
  return zipStore([
    {
      name: '[Content_Types].xml',
      text:
        XML_HEAD +
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
        '<Default Extension="xml" ContentType="application/xml"/>' +
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' +
        so.map((n) => `<Override PartName="/xl/worksheets/sheet${n}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>`).join('') +
        '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>' +
        '</Types>',
    },
    {
      name: '_rels/.rels',
      text:
        XML_HEAD +
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>' +
        '</Relationships>',
    },
    {
      name: 'xl/workbook.xml',
      text:
        XML_HEAD +
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">' +
        `<sheets>${so.map((n) => `<sheet name="${escapeXml(ten[n - 1])}" sheetId="${n}" r:id="rId${n}"/>`).join('')}</sheets></workbook>`,
    },
    {
      name: 'xl/_rels/workbook.xml.rels',
      text:
        XML_HEAD +
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
        so.map((n) => `<Relationship Id="rId${n}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${n}.xml"/>`).join('') +
        `<Relationship Id="rId${so.length + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>` +
        '</Relationships>',
    },
    { name: 'xl/styles.xml', text: isReport ? REPORT_STYLES_XML : STYLES_XML },
    ...sheets.map((sh, i) => ({
      name: `xl/worksheets/sheet${i + 1}.xml`,
      text: isReport ? reportSheetXml(sh, report || {}) : sheetXml(sh.columns, sh.rows),
    })),
  ])
}

/** Tạo file .xlsx và tải về máy. */
export function exportExcel({ filename, sheets, report, ...options }) {
  const url = URL.createObjectURL(sheets ? buildXlsxNhieuSheet(sheets, report) : buildXlsx(options))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
