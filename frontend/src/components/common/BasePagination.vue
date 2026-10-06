<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  total: { type: Number, required: true },
  pageSizeOptions: { type: Array, default: () => [5, 10, 20, 50] },
})
const emit = defineEmits(['update:page', 'update:pageSize'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

// Hiển thị tối đa 5 số trang quanh trang hiện tại.
const pages = computed(() => {
  const maxVisible = 5
  let start = Math.max(1, props.page - Math.floor(maxVisible / 2))
  let end = start + maxVisible - 1
  if (end > totalPages.value) {
    end = totalPages.value
    start = Math.max(1, end - maxVisible + 1)
  }
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

function go(target) {
  const next = Math.min(Math.max(1, target), totalPages.value)
  if (next !== props.page) emit('update:page', next)
}

function onSizeChange(e) {
  emit('update:pageSize', Number(e.target.value))
}
</script>

<template>
  <div class="ad-pager">
    <label class="ad-pager-size">
      <span class="visually-hidden">Số dòng mỗi trang</span>
      <select class="form-select ad-control ad-control-sm" :value="pageSize" @change="onSizeChange">
        <option v-for="n in pageSizeOptions" :key="n" :value="n">{{ n }}</option>
      </select>
    </label>

    <nav aria-label="Phân trang">
      <ul class="ad-pager-list">
        <li>
          <button type="button" class="ad-page-btn" :disabled="page <= 1" aria-label="Trang đầu" @click="go(1)">
            <i class="bi bi-chevron-double-left" aria-hidden="true"></i>
          </button>
        </li>
        <li>
          <button type="button" class="ad-page-btn" :disabled="page <= 1" aria-label="Trang trước" @click="go(page - 1)">
            <i class="bi bi-chevron-left" aria-hidden="true"></i>
          </button>
        </li>
        <li v-for="p in pages" :key="p">
          <button
            type="button"
            class="ad-page-btn"
            :class="{ active: p === page }"
            :aria-current="p === page ? 'page' : undefined"
            :aria-label="`Trang ${p}`"
            @click="go(p)"
          >
            {{ p }}
          </button>
        </li>
        <li>
          <button type="button" class="ad-page-btn" :disabled="page >= totalPages" aria-label="Trang sau" @click="go(page + 1)">
            <i class="bi bi-chevron-right" aria-hidden="true"></i>
          </button>
        </li>
        <li>
          <button type="button" class="ad-page-btn" :disabled="page >= totalPages" aria-label="Trang cuối" @click="go(totalPages)">
            <i class="bi bi-chevron-double-right" aria-hidden="true"></i>
          </button>
        </li>
      </ul>
    </nav>
  </div>
</template>
