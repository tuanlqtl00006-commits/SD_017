import { computed, ref, watch } from 'vue'


export function usePagination(source, initialSize = 5) {
  const page = ref(1)
  const pageSize = ref(initialSize)
  const total = computed(() => source.value.length)
  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  const items = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return source.value.slice(start, start + pageSize.value)
  })

  watch(pageSize, () => {
    page.value = 1
  })
  watch(totalPages, (n) => {
    if (page.value > n) page.value = n
  })

  return { page, pageSize, total, totalPages, items }
}
