<script setup lang="ts">
import { ArrowUpRight, Bell, BookOpenText, Check, Clock3, Lightbulb, ListTodo, StickyNote, Trash2 } from 'lucide-vue-next'
import type { Entry } from '@/types'

defineProps<{ entry: Entry; showTime?: boolean; compact?: boolean }>()
defineEmits<{ complete: [entry: Entry]; remove: [entry: Entry]; open: [entry: Entry] }>()
const labels = { TODO: '待办', DIARY: '日记', IDEA: '想法', NOTE: '备忘' }
const icons = { TODO: ListTodo, DIARY: BookOpenText, IDEA: Lightbulb, NOTE: StickyNote }
const fmt = (value?: string) => value ? new Intl.DateTimeFormat('zh-CN', { month:'numeric', day:'numeric', hour:'2-digit', minute:'2-digit' }).format(new Date(value)) : ''
const reminderText = (entry: Entry) => {
  if (entry.reminderStatus === 'SENT') return `已提醒 ${fmt(entry.reminderSentAt || entry.remindAt)}`
  if (entry.reminderStatus === 'SCHEDULED') return `待提醒 ${fmt(entry.remindAt)}`
  if (entry.reminderStatus === 'PROCESSING') return '正在投递'
  if (entry.reminderStatus === 'FAILED') return '提醒失败'
  if (entry.reminderStatus === 'CANCELLED') return '提醒已取消'
  return ''
}
</script>

<template>
  <article class="entry-card" :class="{ compact, done: entry.status === 'DONE' }" :data-type="entry.type" @click="$emit('open',entry)">
    <div class="type-rail">
      <button v-if="entry.type==='TODO'" class="check" :aria-label="entry.status==='DONE'?'已完成':'标记待办为完成'" @click.stop="$emit('complete',entry)"><Check :size="15" :class="{ 'ghost-check': entry.status!=='DONE' }" /></button>
      <span v-else class="type-glyph"><component :is="icons[entry.type]" :size="16"/></span>
    </div>
    <div class="entry-body">
      <div class="entry-meta"><span class="badge" :class="`badge-${entry.type}`">{{ labels[entry.type] }}</span><span v-if="entry.reminderStatus" class="reminder-badge" :class="`reminder-${entry.reminderStatus.toLowerCase()}`"><Bell :size="11"/>{{reminderText(entry)}}</span><span v-if="showTime" class="entry-time">{{ fmt(entry.occurredAt || entry.createdAt) }}</span><span class="entry-id">RH-{{String(entry.id).padStart(4,'0')}}</span></div>
      <h3 v-if="entry.title">{{ entry.title }}</h3>
      <p v-if="entry.content">{{ entry.content }}</p>
      <span v-if="entry.dueAt" class="due"><Clock3 :size="12" /> {{ fmt(entry.dueAt) }}</span>
    </div>
    <div class="entry-actions"><button class="icon-btn" title="删除" @click.stop="$emit('remove',entry)"><Trash2 :size="15" /></button><span class="open-arrow"><ArrowUpRight :size="16"/></span></div>
  </article>
</template>

<style scoped>
.entry-card{--type-bg:var(--note);--type-fg:var(--note-foreground);position:relative;display:grid;grid-template-columns:40px minmax(0,1fr) auto;gap:14px;align-items:start;padding:18px 17px 18px 13px;overflow:hidden;background:color-mix(in oklch,var(--card) 93%,transparent);border:1px solid color-mix(in oklch,var(--border) 88%,transparent);border-radius:calc(var(--radius)*.82);box-shadow:0 8px 30px color-mix(in oklch,var(--foreground) 3%,transparent);transition:transform .2s,border .2s,box-shadow .2s;cursor:pointer}.entry-card::before{content:"";position:absolute;left:0;top:0;bottom:0;width:3px;background:var(--type-fg);opacity:.65}.entry-card[data-type="TODO"]{--type-bg:var(--todo);--type-fg:var(--todo-foreground)}.entry-card[data-type="DIARY"]{--type-bg:var(--diary);--type-fg:var(--diary-foreground)}.entry-card[data-type="IDEA"]{--type-bg:var(--idea);--type-fg:var(--idea-foreground)}.entry-card+.entry-card{margin-top:10px}.entry-card:hover{transform:translateY(-3px);border-color:color-mix(in oklch,var(--type-fg) 24%,var(--border));box-shadow:0 16px 42px color-mix(in oklch,var(--foreground) 8%,transparent)}.entry-card.compact{padding:14px 15px 14px 11px;grid-template-columns:34px minmax(0,1fr) auto}.entry-card.done{opacity:.62}.entry-card.done h3{text-decoration:line-through}.type-rail{display:flex;justify-content:center}.type-glyph,.check{width:34px;height:34px;border:1px solid color-mix(in oklch,var(--type-fg) 15%,transparent);border-radius:calc(var(--radius)*.55);background:var(--type-bg);color:var(--type-fg);display:grid;place-items:center}.check{cursor:pointer}.done .check{background:var(--type-fg);color:var(--primary-foreground)}.entry-body{min-width:0}.entry-meta{display:flex;align-items:center;gap:9px;margin-bottom:8px}.entry-time{font-size:10px;color:var(--muted-foreground);font-variant-numeric:tabular-nums}.entry-id{margin-left:auto;color:var(--muted-foreground);font-size:8px;font-weight:800;letter-spacing:.08em;opacity:.6}h3{margin:0;font-family:var(--font-display);font-size:16px;line-height:1.4;letter-spacing:-.02em}p{margin:5px 0 0;color:var(--muted-foreground);font-size:12px;line-height:1.7;white-space:pre-wrap;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:3;overflow:hidden}.due{display:inline-flex;align-items:center;gap:5px;margin-top:9px;padding:4px 7px;border-radius:999px;background:var(--muted);color:var(--muted-foreground);font-size:9px;font-weight:800}.entry-actions{display:flex;align-items:center;gap:4px;opacity:0;transform:translateX(6px);transition:.18s}.entry-card:hover .entry-actions{opacity:1;transform:translateX(0)}.open-arrow{width:32px;height:32px;display:grid;place-items:center;color:var(--muted-foreground)}@media(max-width:760px){.entry-card{grid-template-columns:34px minmax(0,1fr) auto;padding:15px 12px 15px 10px}.type-glyph,.check{width:32px;height:32px}.entry-actions{opacity:1;transform:none}.open-arrow{display:none}.entry-id{display:none}}
.reminder-badge{display:inline-flex;align-items:center;gap:5px;padding:5px 8px;border:1px solid var(--border);border-radius:999px;color:var(--muted-foreground);background:var(--muted);font-size:9px;font-weight:900;white-space:nowrap}.reminder-sent{border-color:color-mix(in oklch,var(--success) 30%,var(--border));background:color-mix(in oklch,var(--success) 15%,var(--card));color:var(--success-foreground)}.reminder-scheduled,.reminder-processing{border-color:color-mix(in oklch,var(--reminder-foreground) 20%,var(--border));background:var(--reminder);color:var(--reminder-foreground)}.reminder-failed{border-color:color-mix(in oklch,var(--destructive) 25%,var(--border));background:color-mix(in oklch,var(--destructive) 12%,var(--card));color:var(--destructive)}.reminder-cancelled{opacity:.65}@media(max-width:520px){.entry-meta{flex-wrap:wrap}.reminder-badge{order:3;flex-basis:max-content}}
.ghost-check{opacity:.28;transition:opacity .18s,transform .18s}.check:hover .ghost-check{opacity:1;transform:scale(1.08)}
</style>
