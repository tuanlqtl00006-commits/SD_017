<script setup>
defineProps({
  title: { type: String, default: 'Bộ lọc' },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <section class="ad-card ad-filter-card">
    <header class="ad-card-head">
      <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-funnel"></i></span>
      <div>
        <h2 class="ad-card-title">{{ title }}</h2>
        <p v-if="subtitle" class="ad-card-sub">{{ subtitle }}</p>
      </div>
    </header>
    <div class="ad-filter-body">
      <slot />
    </div>
    <div v-if="$slots.actions" class="ad-filter-actions">
      <slot name="actions" />
    </div>
  </section>
</template>
