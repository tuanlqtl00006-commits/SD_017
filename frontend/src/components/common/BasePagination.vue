<script setup>
// Phân trang rút gọn: 1 2 3 … N (xem utils/phanTrang.js). Dùng chung cho mọi danh sách.
import { computed } from 'vue'
import { danhSachTrang } from '../../utils/phanTrang'

const props = defineProps({
  page: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  total: { type: Number, required: true },
})
const emit = defineEmits(['update:page'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const pages = computed(() => danhSachTrang(props.page, totalPages.value))

function go(target) {
  const next = Math.min(Math.max(1, target), totalPages.value)
  if (next !== props.page) emit('update:page', next)
}
</script>

<template>
  <div class="ad-pager">
    <nav aria-label="Phân trang">
      <ul class="ad-pager-list">
        <li>
          <button type="button" class="ad-page-btn" :disabled="page <= 1" aria-label="Trang trước" @click="go(page - 1)">
            <i class="bi bi-chevron-left" aria-hidden="true"></i>
          </button>
        </li>
        <li v-for="(p, i) in pages" :key="typeof p === 'number' ? p : `gap-${i}`">
          <span v-if="typeof p !== 'number'" class="ad-page-gap" aria-hidden="true">{{ p }}</span>
          <button
            v-else
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
      </ul>
    </nav>
  </div>
</template>
