<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BookOpenText, ChevronLeft, ChevronRight, LoaderCircle, PenLine, Save, Sparkles } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import type { Entry } from '@/types'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EntryCard from '@/components/EntryCard.vue'

interface JournalRecord {
  content?: string
  generatedContent?: string
  generatedAt?: string
  userEdited?: boolean
}

interface JournalData {
  date: string
  journal: JournalRecord
  entries: Entry[]
}

const today = new Date()
const route = useRoute()
const router = useRouter()
const requestedDate = typeof route.query.date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(route.query.date)
  ? route.query.date
  : today.toISOString().slice(0, 10)
const selected = ref(requestedDate)
const data = ref<JournalData | null>(null)
const content = ref('')
const editing = ref(false)
const fragment = ref('')
const busy = ref(false)
const aiBusy = ref(false)
const aiError = ref('')
const deleting = ref<Entry | null>(null)

const title = computed(() => new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long',
}).format(new Date(selected.value + 'T12:00:00')))

const monthDays = computed(() => {
  const date = new Date(selected.value + 'T12:00:00')
  const year = date.getFullYear()
  const month = date.getMonth()
  const count = new Date(year, month + 1, 0).getDate()
  return Array.from({ length: count }, (_, index) =>
    `${year}-${String(month + 1).padStart(2, '0')}-${String(index + 1).padStart(2, '0')}`)
})

async function load() {
  const loaded = await unwrap<JournalData>(api.get(`/journals/${selected.value}`))
  data.value = loaded
  content.value = loaded.journal.content || loaded.journal.generatedContent || ''
  editing.value = false
  aiError.value = ''
}

function shiftMonth(amount: number) {
  const date = new Date(selected.value + 'T12:00:00')
  date.setMonth(date.getMonth() + amount, 1)
  selected.value = date.toISOString().slice(0, 10)
  load()
}

async function save() {
  busy.value = true
  try {
    await unwrap(api.put(`/journals/${selected.value}`, { content: content.value }))
    editing.value = false
    await load()
  } finally {
    busy.value = false
  }
}

async function generateWithAi() {
  if (!data.value?.entries.length || aiBusy.value) return
  aiBusy.value = true
  aiError.value = ''
  try {
    const journal = await unwrap<JournalRecord>(api.post(
      `/journals/${selected.value}/generate`,
      undefined,
      { timeout: 630_000 },
    ))
    if (data.value) data.value.journal = journal
    content.value = journal.content || journal.generatedContent || ''
    editing.value = false
  } catch (error: any) {
    aiError.value = error.userMessage || 'AI 整理失败，请稍后重试'
  } finally {
    aiBusy.value = false
  }
}

function openReader() {
  if (content.value) router.push({ name: 'journal-reader', params: { date: selected.value } })
}

async function addFragment() {
  if (!fragment.value.trim()) return
  await unwrap(api.post('/entries', {
    type: 'DIARY',
    content: fragment.value,
    occurredAt: new Date(selected.value + 'T12:00:00').toISOString(),
  }))
  fragment.value = ''
  await load()
}

function remove(entry: Entry) {
  deleting.value = entry
}

async function confirmRemove() {
  if (!deleting.value) return
  await api.delete(`/entries/${deleting.value.id}`)
  deleting.value = null
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page journal-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">DAILY PAGES</p>
        <h1 class="page-title">日记</h1>
        <p class="page-subtitle">原始片段一直保留，整理后的文字属于这一天。</p>
      </div>
    </header>

    <div class="journal-layout">
      <aside class="calendar card">
        <div class="calendar-head">
          <button class="icon-btn" aria-label="上个月" @click="shiftMonth(-1)"><ChevronLeft :size="17" /></button>
          <strong>{{ new Date(selected + 'T12:00:00').toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' }) }}</strong>
          <button class="icon-btn" aria-label="下个月" @click="shiftMonth(1)"><ChevronRight :size="17" /></button>
        </div>
        <div class="week"><span v-for="week in '一二三四五六日'" :key="week">{{ week }}</span></div>
        <div class="days">
          <span v-for="blank in (new Date(monthDays[0] + 'T12:00:00').getDay() + 6) % 7" :key="'blank-' + blank" />
          <button
            v-for="day in monthDays"
            :key="day"
            :class="{ active: day === selected, today: day === new Date().toISOString().slice(0, 10) }"
            @click="selected = day; load()"
          >{{ Number(day.slice(-2)) }}</button>
        </div>
      </aside>

      <main class="journal-content">
        <div class="journal-title">
          <div>
            <p class="eyebrow">SELECTED DAY</p>
            <h2>{{ title }}</h2>
          </div>
          <div class="journal-actions">
            <button class="btn reader-entry" :disabled="!content" title="全屏阅读整理后的日记" @click="openReader">
              <BookOpenText :size="15" />沉浸式看日记
            </button>
            <button
              class="btn ai-organize"
              :disabled="aiBusy || !data?.entries.length"
              :title="!data?.entries.length ? '先添加原始片段' : '用设置中的 AI 模型整理当天片段'"
              @click="generateWithAi"
            >
              <LoaderCircle v-if="aiBusy" class="spin" :size="15" />
              <Sparkles v-else :size="15" />
              {{ aiBusy ? '正在整理…' : (content ? 'AI 重新整理' : 'AI 一键整理') }}
            </button>
            <button class="btn btn-secondary" @click="editing = !editing">
              <PenLine :size="15" />{{ editing ? '查看' : '手动整理' }}
            </button>
          </div>
        </div>

        <p v-if="aiError" class="ai-error">
          {{ aiError }}
          <RouterLink v-if="aiError.includes('配置')" to="/settings?section=ai">前往 AI 设置</RouterLink>
        </p>

        <section class="written card">
          <div class="section-head">
            <h3 class="section-title">整理后的日记</h3>
            <span v-if="data?.journal.generatedAt" class="ai-stamp"><Sparkles :size="11" />AI 已整理</span>
          </div>
          <textarea v-if="editing" v-model="content" class="journal-editor" placeholder="把这一天整理成一篇日记……" />
          <div v-else-if="content" class="journal-text">{{ content }}</div>
          <div v-else class="empty">这一天还没有整理后的日记。</div>
          <button v-if="editing" class="btn btn-primary save" :disabled="busy" @click="save">
            <Save :size="15" />保存日记
          </button>
        </section>

        <section class="section">
          <div class="section-head">
            <h3 class="section-title">原始片段</h3>
            <span class="count">{{ data?.entries.length || 0 }} 条</span>
          </div>
          <form class="fragment card" @submit.prevent="addFragment">
            <textarea v-model="fragment" placeholder="补记这一刻……" />
            <button class="btn btn-primary">记下</button>
          </form>
          <EntryCard v-for="entry in data?.entries" :key="entry.id" :entry="entry" show-time @remove="remove" />
        </section>
      </main>
    </div>
    <ConfirmDialog
      v-if="deleting"
      title="删除这条日记片段？"
      message="删除后，这条原始片段将不会再出现在日记、今天和时间线中。"
      confirm-text="删除"
      @cancel="deleting = null"
      @confirm="confirmRemove"
    />
  </div>
</template>

<style scoped>
.journal-page{max-width:1280px}.journal-layout{display:grid;grid-template-columns:310px minmax(0,1fr);gap:42px;align-items:start}.calendar{position:sticky;top:28px;padding:22px 18px 20px;overflow:hidden;background:var(--primary);color:var(--primary-foreground);border-color:color-mix(in oklch,var(--primary-foreground) 12%,transparent);box-shadow:0 24px 60px color-mix(in oklch,var(--primary) 22%,transparent)}.calendar::after{content:"";position:absolute;width:160px;height:160px;right:-82px;bottom:-92px;border:30px solid color-mix(in oklch,var(--primary-foreground) 8%,transparent);border-radius:50%;pointer-events:none}.calendar-head{position:relative;z-index:1;display:flex;align-items:center;justify-content:space-between;font-size:11px}.calendar :deep(.icon-btn){color:var(--primary-foreground)}.calendar :deep(.icon-btn:hover){background:color-mix(in oklch,var(--primary-foreground) 12%,transparent);border-color:transparent}.week,.days{position:relative;z-index:1;display:grid;grid-template-columns:repeat(7,1fr);gap:5px}.week{margin:20px 0 8px;color:color-mix(in oklch,var(--primary-foreground) 55%,transparent);font-size:8px;font-weight:900;text-align:center}.days button{aspect-ratio:1;border:0;background:transparent;color:inherit;border-radius:calc(var(--radius)*.45);font-size:10px;transition:.16s}.days button:hover{background:color-mix(in oklch,var(--primary-foreground) 12%,transparent)}.days button.today{box-shadow:inset 0 0 0 1px color-mix(in oklch,var(--primary-foreground) 50%,transparent)}.days button.active{background:var(--primary-foreground);color:var(--primary);box-shadow:0 8px 20px color-mix(in oklch,var(--foreground) 22%,transparent)}.journal-title{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin:0 0 20px}.journal-title h2{font:650 31px/1.2 var(--font-display);letter-spacing:-.045em;margin:8px 0 0}.journal-actions{display:flex;gap:9px;flex-wrap:wrap;justify-content:flex-end}.reader-entry{border:1px solid color-mix(in oklch,var(--foreground) 12%,var(--border));background:var(--foreground);color:var(--background);box-shadow:0 9px 24px color-mix(in oklch,var(--foreground) 13%,transparent)}.reader-entry:disabled{opacity:.38}.ai-organize{position:relative;overflow:hidden;border:1px solid color-mix(in oklch,var(--primary) 35%,var(--border));background:color-mix(in oklch,var(--primary) 9%,var(--card));color:var(--primary);box-shadow:0 9px 24px color-mix(in oklch,var(--primary) 10%,transparent)}.ai-organize::after{content:"";position:absolute;inset:0;transform:translateX(-120%);background:linear-gradient(100deg,transparent,color-mix(in oklch,var(--primary-foreground) 34%,transparent),transparent);transition:transform .55s}.ai-organize:hover::after{transform:translateX(120%)}.ai-organize:disabled::after{display:none}.ai-error{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:-8px 0 14px;padding:11px 14px;border:1px solid color-mix(in oklch,var(--destructive) 28%,var(--border));border-radius:calc(var(--radius)*.65);background:color-mix(in oklch,var(--destructive) 7%,var(--card));color:var(--destructive);font-size:11px}.ai-error a{color:inherit;font-weight:900;white-space:nowrap}.written{position:relative;min-height:330px;padding:30px 32px;overflow:hidden;background:repeating-linear-gradient(to bottom,color-mix(in oklch,var(--card) 94%,transparent) 0,color-mix(in oklch,var(--card) 94%,transparent) 35px,color-mix(in oklch,var(--border) 60%,transparent) 36px)}.written::before{content:"";position:absolute;left:18px;top:0;bottom:0;width:1px;background:color-mix(in oklch,var(--destructive) 18%,transparent)}.written .section-head{position:relative;z-index:1}.ai-stamp{display:inline-flex;align-items:center;gap:5px;color:var(--primary);font-size:9px;font-weight:900;letter-spacing:.08em}.journal-text{position:relative;z-index:1;font-family:var(--font-display);white-space:pre-wrap;line-height:2.4;font-size:15px}.journal-editor{position:relative;z-index:1;width:100%;min-height:270px;border:0;background:transparent;outline:0;resize:vertical;font-family:var(--font-display);font-size:15px;line-height:2.4}.written :deep(.empty){background:color-mix(in oklch,var(--card) 70%,transparent)}.save{position:relative;z-index:1;margin-top:17px}.fragment{display:flex;gap:10px;padding:10px 10px 10px 18px;margin-bottom:13px;border-color:color-mix(in oklch,var(--diary-foreground) 16%,var(--border))}.fragment textarea{flex:1;min-height:46px;padding:10px 0;border:0;background:transparent;resize:none;outline:0;font-family:var(--font-display);line-height:1.7}.fragment .btn{align-self:flex-end}.spin{animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:960px){.journal-layout{grid-template-columns:270px minmax(0,1fr);gap:25px}.journal-title{align-items:flex-start;flex-direction:column}.journal-actions{justify-content:flex-start}}@media(max-width:760px){.journal-layout{grid-template-columns:1fr}.calendar{position:static}.journal-page{max-width:720px}.written{padding:24px 22px}.journal-title h2{font-size:25px}}@media(max-width:480px){.journal-actions{width:100%}.journal-actions .btn{flex:1}.ai-error{align-items:flex-start;flex-direction:column}}
</style>
