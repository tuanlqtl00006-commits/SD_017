import { reactive } from 'vue'
import { thuocTinhService } from '../services/thuocTinhService'


export function useThuocTinh(types) {
  const data = reactive(Object.fromEntries(types.map((t) => [t, []])))

  async function loadThuocTinh() {
    const results = await Promise.all(types.map((t) => thuocTinhService.getAll(t)))
    types.forEach((t, i) => { data[t] = results[i] })
  }

  
  const tenOf = (type, id) => data[type].find((x) => x.id === id)?.ten ?? '—'

  
  const optionsOf = (type, currentId = null) => data[type].filter((x) => x.hoatDong || x.id === currentId)

  return { data, loadThuocTinh, tenOf, optionsOf }
}
