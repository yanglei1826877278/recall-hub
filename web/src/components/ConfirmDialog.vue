<script setup lang="ts">
import { AlertTriangle, X } from 'lucide-vue-next'
defineProps<{ title?: string; message: string; confirmText?: string }>()
defineEmits<{ confirm: []; cancel: [] }>()
</script>

<template>
  <Teleport to="body">
    <div class="confirm-backdrop" @mousedown.self="$emit('cancel')">
      <section class="confirm-card card" role="alertdialog" aria-modal="true">
        <button class="icon-btn close" aria-label="关闭" @click="$emit('cancel')"><X :size="18"/></button>
        <div class="danger-icon"><AlertTriangle :size="22"/></div>
        <h2>{{title || '确认操作'}}</h2>
        <p>{{message}}</p>
        <div class="confirm-actions"><button class="btn btn-secondary" @click="$emit('cancel')">取消</button><button class="btn btn-danger solid" @click="$emit('confirm')">{{confirmText || '确认'}}</button></div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.confirm-backdrop{position:fixed;inset:0;z-index:90;display:grid;place-items:center;padding:20px;background:color-mix(in oklch,var(--foreground) 25%,transparent);backdrop-filter:blur(8px);animation:fade .18s both}.confirm-card{position:relative;width:min(390px,100%);padding:28px;text-align:center;animation:pop .22s cubic-bezier(.2,.9,.2,1) both}.close{position:absolute;right:12px;top:12px}.danger-icon{width:48px;height:48px;margin:0 auto 14px;display:grid;place-items:center;border-radius:16px;background:color-mix(in oklch,var(--destructive) 13%,transparent);color:var(--destructive)}h2{margin:0;font:600 22px/1.3 "Noto Serif SC","Songti SC",serif}p{margin:9px 0 24px;color:var(--muted-foreground);font-size:13px;line-height:1.65}.confirm-actions{display:grid;grid-template-columns:1fr 1fr;gap:9px}.solid{background:var(--destructive);color:white;box-shadow:0 8px 22px color-mix(in oklch,var(--destructive) 20%,transparent)}
</style>

