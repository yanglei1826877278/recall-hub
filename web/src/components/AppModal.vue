<script setup lang="ts">
import { X } from 'lucide-vue-next'
defineProps<{ title: string }>()
defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @mousedown.self="$emit('close')">
      <section class="modal card" role="dialog" aria-modal="true">
        <header class="modal-head"><h2 class="modal-title">{{ title }}</h2><button class="icon-btn" aria-label="关闭" @click="$emit('close')"><X :size="20" /></button></header>
        <slot />
      </section>
    </div>
  </Teleport>
</template>

