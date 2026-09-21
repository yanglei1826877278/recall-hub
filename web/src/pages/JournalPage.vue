<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BookOpenText, Check, ChevronDown, ChevronLeft, ChevronRight, Copy, ExternalLink, KeyRound, ListTree, LoaderCircle, PenLine, RefreshCw, Save, Share2, ShieldCheck, Sparkles, X } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import type { Entry } from '@/types'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EntryCard from '@/components/EntryCard.vue'
import AppModal from '@/components/AppModal.vue'

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

interface ShareStatus {
  active: boolean
  sharePath?: string
  sharedAt?: string
  snapshotUpdatedAt?: string
  contentOutdated: boolean
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
const organizeSheetOpen = ref(false)
const writtenSection = ref<HTMLElement | null>(null)
const sourceSection = ref<HTMLElement | null>(null)
const journalEditor = ref<HTMLTextAreaElement | null>(null)
const savedContent = ref('')
const shareOpen = ref(false)
const shareLoading = ref(false)
const shareBusy = ref(false)
const shareStatus = ref<ShareStatus | null>(null)
const sharePassword = ref('')
const sharePasswordConfirm = ref('')
const changingPassword = ref(false)
const shareError = ref('')
const copied = ref(false)
const revokeConfirm = ref(false)

const hasUnsavedContent = computed(() => editing.value && content.value !== savedContent.value)
const shareUrl = computed(() => shareStatus.value?.sharePath ? `${location.origin}${shareStatus.value.sharePath}` : '')
const shareContentOutdated = computed(() => Boolean(shareStatus.value?.contentOutdated || hasUnsavedContent.value))

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
  savedContent.value = content.value
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
    savedContent.value = content.value
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

async function openShare() {
  shareOpen.value = true
  shareLoading.value = true
  shareError.value = hasUnsavedContent.value ? '正文还有未保存的修改，请保存后再创建或更新分享。' : ''
  sharePassword.value = ''
  sharePasswordConfirm.value = ''
  changingPassword.value = false
  copied.value = false
  try {
    shareStatus.value = await unwrap<ShareStatus>(api.get(`/journals/${selected.value}/share`))
  } catch (error: any) {
    shareError.value = error.userMessage || '分享状态加载失败'
  } finally {
    shareLoading.value = false
  }
}

function closeShare() {
  if (shareBusy.value) return
  shareOpen.value = false
  changingPassword.value = false
  shareError.value = ''
}

function normalizeSharePassword() {
  sharePassword.value = sharePassword.value.replace(/\D/g, '').slice(0, 4)
  shareError.value = ''
}

async function submitSharePassword() {
  if (hasUnsavedContent.value && !shareStatus.value?.active) {
    shareError.value = '请先保存正文，再分享这一页。'
    return
  }
  if (!/^\d{4}$/.test(sharePassword.value)) {
    shareError.value = '查看密码必须是 4 位数字。'
    return
  }
  if (sharePassword.value !== sharePasswordConfirm.value) {
    shareError.value = '两次输入的密码不一致。'
    return
  }
  shareBusy.value = true
  shareError.value = ''
  try {
    const request = shareStatus.value?.active
      ? api.patch(`/journals/${selected.value}/share/password`, { password: sharePassword.value })
      : api.post(`/journals/${selected.value}/share`, { password: sharePassword.value })
    shareStatus.value = await unwrap<ShareStatus>(request)
    sharePassword.value = ''
    sharePasswordConfirm.value = ''
    changingPassword.value = false
    await copyShareLink()
  } catch (error: any) {
    shareError.value = error.userMessage || '分享设置保存失败'
  } finally {
    shareBusy.value = false
  }
}

async function copyShareLink() {
  if (!shareUrl.value) return
  try {
    await navigator.clipboard.writeText(shareUrl.value)
  } catch {
    const input = document.createElement('textarea')
    input.value = shareUrl.value
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    input.remove()
  }
  copied.value = true
  window.setTimeout(() => { copied.value = false }, 1800)
}

async function refreshShareContent() {
  if (hasUnsavedContent.value) {
    shareError.value = '请先保存正文，再更新公开快照。'
    return
  }
  shareBusy.value = true
  shareError.value = ''
  try {
    shareStatus.value = await unwrap<ShareStatus>(api.put(`/journals/${selected.value}/share/content`))
  } catch (error: any) {
    shareError.value = error.userMessage || '公开快照更新失败'
  } finally {
    shareBusy.value = false
  }
}

async function revokeShare() {
  shareBusy.value = true
  try {
    await api.delete(`/journals/${selected.value}/share`)
    revokeConfirm.value = false
    shareOpen.value = false
    shareStatus.value = { active: false, contentOutdated: false }
  } catch (error: any) {
    shareError.value = error.userMessage || '取消分享失败'
    revokeConfirm.value = false
  } finally {
    shareBusy.value = false
  }
}

function formatShareTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : ''
}

async function organizeWithAi() {
  organizeSheetOpen.value = false
  await generateWithAi()
}

async function startManualOrganize() {
  organizeSheetOpen.value = false
  editing.value = true
  await nextTick()
  writtenSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  journalEditor.value?.focus({ preventScroll: true })
}

async function viewSourceEntries() {
  organizeSheetOpen.value = false
  await nextTick()
  sourceSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
            <div class="desktop-journal-actions">
              <button class="btn share-entry" :disabled="!content" title="用密码分享这一天" @click="openShare">
                <Share2 :size="15" />分享这一天
              </button>
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
            <div class="mobile-journal-actions">
              <button class="btn share-entry" :disabled="!content" @click="openShare">
                <Share2 :size="17" />分享
              </button>
              <button class="btn reader-entry" :disabled="!content" @click="openReader">
                <BookOpenText :size="17" />沉浸阅读
              </button>
              <button class="btn organize-menu-trigger" aria-haspopup="dialog" :aria-expanded="organizeSheetOpen" @click="organizeSheetOpen = true">
                整理<ChevronDown :size="16" />
              </button>
            </div>
          </div>
        </div>

        <p v-if="aiError" class="ai-error">
          {{ aiError }}
          <RouterLink v-if="aiError.includes('配置')" to="/settings?section=ai">前往 AI 设置</RouterLink>
        </p>

        <section ref="writtenSection" class="written card">
          <div class="section-head">
            <h3 class="section-title">整理后的日记</h3>
            <span v-if="data?.journal.generatedAt" class="ai-stamp"><Sparkles :size="11" />AI 已整理</span>
          </div>
          <textarea v-if="editing" ref="journalEditor" v-model="content" class="journal-editor" placeholder="把这一天整理成一篇日记……" />
          <div v-else-if="content" class="journal-text">{{ content }}</div>
          <div v-else class="empty">这一天还没有整理后的日记。</div>
          <button v-if="editing" class="btn btn-primary save" :disabled="busy" @click="save">
            <Save :size="15" />保存日记
          </button>
        </section>

        <section ref="sourceSection" class="section source-section">
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
    <AppModal v-if="shareOpen" title="分享这一天" @close="closeShare">
      <div v-if="shareLoading" class="share-loading"><LoaderCircle class="spin" :size="21" />正在检查分享状态…</div>
      <div v-else class="share-panel">
        <div class="share-intro">
          <span class="share-intro-icon"><ShieldCheck :size="22" /></span>
          <div>
            <p class="eyebrow">ONE DAY · ONE PAGE</p>
            <h3>{{ title }}</h3>
            <p>公开页面只保存这一天的正文快照，不包含原始片段和其他日期。</p>
          </div>
        </div>

        <template v-if="shareStatus?.active">
          <div class="share-live">
            <span class="live-dot" />
            <div><strong>正在分享</strong><small>密码保护已开启</small></div>
            <a :href="shareUrl" target="_blank" rel="noopener noreferrer" aria-label="打开分享页"><ExternalLink :size="17" /></a>
          </div>

          <div class="share-link-row">
            <input :value="shareUrl" readonly aria-label="分享链接" @focus="($event.target as HTMLInputElement).select()" />
            <button class="btn btn-primary" @click="copyShareLink"><Check v-if="copied" :size="16"/><Copy v-else :size="16"/>{{ copied ? '已复制' : '复制' }}</button>
          </div>

          <div :class="['snapshot-status', { outdated: shareContentOutdated }]">
            <RefreshCw :size="17" />
            <div>
              <strong>{{ shareContentOutdated ? '原日记已有新修改' : '公开快照与当前正文一致' }}</strong>
              <small>快照更新于 {{ formatShareTime(shareStatus.snapshotUpdatedAt) }}</small>
            </div>
            <button v-if="shareContentOutdated" :disabled="shareBusy || hasUnsavedContent" @click="refreshShareContent">更新快照</button>
          </div>

          <form v-if="changingPassword" class="password-form" @submit.prevent="submitSharePassword">
            <div class="password-heading"><KeyRound :size="18"/><div><strong>设置新的查看密码</strong><small>修改后，已解锁的设备也要重新输入</small></div></div>
            <div class="password-fields">
              <label><span>新密码</span><input v-model="sharePassword" type="password" inputmode="numeric" maxlength="4" autocomplete="new-password" placeholder="4 位数字" @input="normalizeSharePassword" /></label>
              <label><span>再次输入</span><input v-model="sharePasswordConfirm" type="password" inputmode="numeric" maxlength="4" autocomplete="new-password" placeholder="确认密码" /></label>
            </div>
            <div class="password-actions"><button type="button" class="btn btn-secondary" @click="changingPassword = false; shareError = ''">取消</button><button class="btn btn-primary" :disabled="shareBusy"><LoaderCircle v-if="shareBusy" class="spin" :size="15"/>保存新密码</button></div>
          </form>

          <p v-if="shareError" class="share-error">{{ shareError }}</p>
          <div v-if="!changingPassword" class="share-actions">
            <button class="btn btn-secondary" @click="changingPassword = true"><KeyRound :size="15"/>修改密码</button>
            <button class="btn btn-danger" @click="revokeConfirm = true"><X :size="15"/>取消分享</button>
          </div>
        </template>

        <form v-else class="create-share" @submit.prevent="submitSharePassword">
          <div class="password-heading"><KeyRound :size="18"/><div><strong>设置查看密码</strong><small>必须填写 4 位数字，可以包含开头的 0</small></div></div>
          <div class="password-fields">
            <label><span>查看密码</span><input v-model="sharePassword" autofocus type="password" inputmode="numeric" maxlength="4" autocomplete="new-password" placeholder="例如 0123" @input="normalizeSharePassword" /></label>
            <label><span>再次输入</span><input v-model="sharePasswordConfirm" type="password" inputmode="numeric" maxlength="4" autocomplete="new-password" placeholder="确认密码" /></label>
          </div>
          <p v-if="shareError" class="share-error">{{ shareError }}</p>
          <button class="btn btn-primary create-share-button" :disabled="shareBusy || hasUnsavedContent"><LoaderCircle v-if="shareBusy" class="spin" :size="16"/><Share2 v-else :size="16"/>{{ shareBusy ? '正在创建…' : '创建分享链接' }}</button>
        </form>
      </div>
    </AppModal>
    <Teleport to="body">
      <Transition name="organize-sheet">
        <div v-if="organizeSheetOpen" class="organize-sheet-backdrop" @click.self="organizeSheetOpen = false">
          <section class="organize-sheet" role="dialog" aria-modal="true" aria-labelledby="organize-sheet-title">
            <span class="sheet-handle" aria-hidden="true" />
            <header class="sheet-head">
              <div><p>JOURNAL TOOLS</p><h2 id="organize-sheet-title">整理这一天</h2></div>
              <button class="icon-btn" aria-label="关闭整理菜单" @click="organizeSheetOpen = false"><X :size="18" /></button>
            </header>
            <div class="sheet-actions">
              <button class="sheet-action ai" :disabled="aiBusy || !data?.entries.length" @click="organizeWithAi">
                <span class="sheet-action-icon"><LoaderCircle v-if="aiBusy" class="spin" :size="19"/><Sparkles v-else :size="19"/></span>
                <span><strong>{{aiBusy?'正在整理…':(content?'AI 重新整理':'AI 一键整理')}}</strong><small>{{data?.entries.length?'根据当天的原始片段重新生成正文':'先添加一条原始记录'}}</small></span>
                <ChevronRight :size="17" />
              </button>
              <button class="sheet-action" @click="startManualOrganize">
                <span class="sheet-action-icon"><PenLine :size="19"/></span>
                <span><strong>手动整理</strong><small>直接编辑整理后的日记正文</small></span>
                <ChevronRight :size="17" />
              </button>
              <button class="sheet-action" @click="viewSourceEntries">
                <span class="sheet-action-icon"><ListTree :size="19"/></span>
                <span><strong>查看原始记录</strong><small>跳到这一天保留的全部片段</small></span>
                <ChevronRight :size="17" />
              </button>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>
    <ConfirmDialog
      v-if="revokeConfirm"
      title="取消这一天的分享？"
      message="取消后链接会立即失效，已经解锁的设备也无法继续查看。以后重新分享会生成全新的链接。"
      confirm-text="取消分享"
      @cancel="revokeConfirm = false"
      @confirm="revokeShare"
    />
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
.journal-page{max-width:1280px}.journal-layout{display:grid;grid-template-columns:310px minmax(0,1fr);gap:42px;align-items:start}.calendar{position:sticky;top:28px;padding:22px 18px 20px;overflow:hidden;background:var(--primary);color:var(--primary-foreground);border-color:color-mix(in oklch,var(--primary-foreground) 12%,transparent);box-shadow:0 24px 60px color-mix(in oklch,var(--primary) 22%,transparent)}.calendar::after{content:"";position:absolute;width:160px;height:160px;right:-82px;bottom:-92px;border:30px solid color-mix(in oklch,var(--primary-foreground) 8%,transparent);border-radius:50%;pointer-events:none}.calendar-head{position:relative;z-index:1;display:flex;align-items:center;justify-content:space-between;font-size:11px}.calendar :deep(.icon-btn){color:var(--primary-foreground)}.calendar :deep(.icon-btn:hover){background:color-mix(in oklch,var(--primary-foreground) 12%,transparent);border-color:transparent}.week,.days{position:relative;z-index:1;display:grid;grid-template-columns:repeat(7,1fr);gap:5px}.week{margin:20px 0 8px;color:color-mix(in oklch,var(--primary-foreground) 55%,transparent);font-size:8px;font-weight:900;text-align:center}.days button{aspect-ratio:1;border:0;background:transparent;color:inherit;border-radius:calc(var(--radius)*.45);font-size:10px;transition:.16s}.days button:hover{background:color-mix(in oklch,var(--primary-foreground) 12%,transparent)}.days button.today{box-shadow:inset 0 0 0 1px color-mix(in oklch,var(--primary-foreground) 50%,transparent)}.days button.active{background:var(--primary-foreground);color:var(--primary);box-shadow:0 8px 20px color-mix(in oklch,var(--foreground) 22%,transparent)}.journal-title{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin:0 0 20px}.journal-title h2{font:650 31px/1.2 var(--font-display);letter-spacing:-.045em;margin:8px 0 0}.journal-actions{display:flex;gap:9px;flex-wrap:wrap;justify-content:flex-end}.desktop-journal-actions{display:flex;gap:9px;flex-wrap:wrap;justify-content:flex-end}.mobile-journal-actions{display:none}.reader-entry{border:1px solid color-mix(in oklch,var(--foreground) 12%,var(--border));background:var(--foreground);color:var(--background);box-shadow:0 9px 24px color-mix(in oklch,var(--foreground) 13%,transparent)}.reader-entry:disabled{opacity:.38}.ai-organize{position:relative;overflow:hidden;border:1px solid color-mix(in oklch,var(--primary) 35%,var(--border));background:color-mix(in oklch,var(--primary) 9%,var(--card));color:var(--primary);box-shadow:0 9px 24px color-mix(in oklch,var(--primary) 10%,transparent)}.ai-organize::after{content:"";position:absolute;inset:0;transform:translateX(-120%);background:linear-gradient(100deg,transparent,color-mix(in oklch,var(--primary-foreground) 34%,transparent),transparent);transition:transform .55s}.ai-organize:hover::after{transform:translateX(120%)}.ai-organize:disabled::after{display:none}.ai-error{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:-8px 0 14px;padding:11px 14px;border:1px solid color-mix(in oklch,var(--destructive) 28%,var(--border));border-radius:calc(var(--radius)*.65);background:color-mix(in oklch,var(--destructive) 7%,var(--card));color:var(--destructive);font-size:11px}.ai-error a{color:inherit;font-weight:900;white-space:nowrap}.written{position:relative;min-height:330px;padding:30px 32px;overflow:hidden;background:repeating-linear-gradient(to bottom,color-mix(in oklch,var(--card) 94%,transparent) 0,color-mix(in oklch,var(--card) 94%,transparent) 35px,color-mix(in oklch,var(--border) 60%,transparent) 36px)}.written::before{content:"";position:absolute;left:18px;top:0;bottom:0;width:1px;background:color-mix(in oklch,var(--destructive) 18%,transparent)}.written .section-head{position:relative;z-index:1}.ai-stamp{display:inline-flex;align-items:center;gap:5px;color:var(--primary);font-size:9px;font-weight:900;letter-spacing:.08em}.journal-text{position:relative;z-index:1;font-family:var(--font-display);white-space:pre-wrap;line-height:2.4;font-size:15px}.journal-editor{position:relative;z-index:1;width:100%;min-height:270px;border:0;background:transparent;outline:0;resize:vertical;font-family:var(--font-display);font-size:15px;line-height:2.4}.written :deep(.empty){background:color-mix(in oklch,var(--card) 70%,transparent)}.save{position:relative;z-index:1;margin-top:17px}.fragment{display:flex;gap:10px;padding:10px 10px 10px 18px;margin-bottom:13px;border-color:color-mix(in oklch,var(--diary-foreground) 16%,var(--border))}.fragment textarea{flex:1;min-height:46px;padding:10px 0;border:0;background:transparent;resize:none;outline:0;font-family:var(--font-display);line-height:1.7}.fragment .btn{align-self:flex-end}.source-section{scroll-margin-top:18px}.organize-sheet-backdrop{position:fixed;inset:0;z-index:120;display:flex;align-items:flex-end;justify-content:center;padding:14px 12px calc(82px + env(safe-area-inset-bottom));background:color-mix(in oklch,var(--foreground) 30%,transparent);backdrop-filter:blur(8px)}.organize-sheet{width:min(560px,100%);padding:9px 16px 16px;border:1px solid color-mix(in oklch,var(--border) 84%,transparent);border-radius:26px 26px 18px 18px;background:var(--card);box-shadow:0 -24px 70px color-mix(in oklch,var(--foreground) 20%,transparent)}.sheet-handle{display:block;width:42px;height:4px;margin:0 auto 13px;border-radius:99px;background:color-mix(in oklch,var(--muted-foreground) 26%,transparent)}.sheet-head{display:flex;align-items:center;justify-content:space-between;padding:0 4px 14px}.sheet-head p{margin:0 0 4px;color:var(--primary);font-size:8px;font-weight:900;letter-spacing:.17em}.sheet-head h2{margin:0;font:650 21px/1.25 var(--font-display);letter-spacing:-.025em}.sheet-actions{display:grid;gap:8px}.sheet-action{display:grid;grid-template-columns:44px minmax(0,1fr) auto;align-items:center;gap:12px;width:100%;min-height:67px;padding:9px 12px;border:1px solid var(--border);border-radius:calc(var(--radius)*.78);background:var(--background);color:var(--foreground);text-align:left}.sheet-action:hover{border-color:color-mix(in oklch,var(--primary) 30%,var(--border));background:color-mix(in oklch,var(--primary) 4%,var(--background))}.sheet-action:disabled{opacity:.45}.sheet-action.ai{border-color:color-mix(in oklch,var(--primary) 20%,var(--border));background:color-mix(in oklch,var(--primary) 7%,var(--background))}.sheet-action-icon{width:42px;height:42px;display:grid;place-items:center;border-radius:13px;background:var(--muted);color:var(--muted-foreground)}.sheet-action.ai .sheet-action-icon{background:var(--primary);color:var(--primary-foreground);box-shadow:0 8px 20px color-mix(in oklch,var(--primary) 22%,transparent)}.sheet-action>span:nth-child(2){display:grid;gap:4px;min-width:0}.sheet-action strong{font-size:12px}.sheet-action small{overflow:hidden;color:var(--muted-foreground);font-size:9px;text-overflow:ellipsis;white-space:nowrap}.sheet-action>svg{color:var(--muted-foreground)}.organize-sheet-enter-active,.organize-sheet-leave-active{transition:opacity .2s}.organize-sheet-enter-active .organize-sheet,.organize-sheet-leave-active .organize-sheet{transition:transform .26s cubic-bezier(.2,.85,.2,1)}.organize-sheet-enter-from,.organize-sheet-leave-to{opacity:0}.organize-sheet-enter-from .organize-sheet,.organize-sheet-leave-to .organize-sheet{transform:translateY(110%)}.spin{animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:960px){.journal-layout{grid-template-columns:270px minmax(0,1fr);gap:25px}.journal-title{align-items:flex-start;flex-direction:column}.journal-actions{justify-content:flex-start}}@media(max-width:760px){.journal-layout{grid-template-columns:1fr}.calendar{position:static}.journal-page{max-width:720px}.written{padding:24px 22px}.journal-title h2{font-size:25px}}@media(max-width:680px){.journal-actions{width:100%}.desktop-journal-actions{display:none}.mobile-journal-actions{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(112px,.65fr);gap:10px;width:100%}.mobile-journal-actions .btn{min-height:52px}.organize-menu-trigger{border:1px solid color-mix(in oklch,var(--primary) 22%,var(--border));background:color-mix(in oklch,var(--primary) 7%,var(--card));color:var(--primary)}.ai-error{align-items:flex-start;flex-direction:column}}
.share-entry{border:1px solid color-mix(in oklch,var(--diary-foreground) 24%,var(--border));background:color-mix(in oklch,var(--diary) 58%,var(--card));color:var(--diary-foreground)}.share-entry:disabled{opacity:.38}.share-loading{min-height:250px;display:flex;align-items:center;justify-content:center;gap:10px;color:var(--muted-foreground);font-size:12px}.share-panel{display:grid;gap:18px}.share-intro{position:relative;display:grid;grid-template-columns:54px minmax(0,1fr);gap:15px;align-items:start;padding:18px;overflow:hidden;border:1px solid color-mix(in oklch,var(--diary-foreground) 15%,var(--border));border-radius:calc(var(--radius)*.78);background:linear-gradient(135deg,color-mix(in oklch,var(--diary) 52%,var(--card)),var(--card))}.share-intro::after{content:"";position:absolute;width:110px;height:110px;right:-50px;bottom:-68px;border:18px solid color-mix(in oklch,var(--diary-foreground) 7%,transparent);border-radius:50%}.share-intro-icon{width:50px;height:50px;display:grid;place-items:center;border-radius:16px;background:var(--diary-foreground);color:var(--primary-foreground);box-shadow:0 10px 26px color-mix(in oklch,var(--diary-foreground) 20%,transparent)}.share-intro .eyebrow{font-size:8px;color:var(--diary-foreground)}.share-intro h3{margin:7px 0 5px;font:650 19px/1.3 var(--font-display);letter-spacing:-.025em}.share-intro p:last-child{margin:0;color:var(--muted-foreground);font-size:10px;line-height:1.65}.share-live{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:11px;padding:4px 2px}.live-dot{width:9px;height:9px;border-radius:50%;background:var(--success);box-shadow:0 0 0 5px color-mix(in oklch,var(--success) 13%,transparent)}.share-live>div{display:grid;gap:3px}.share-live strong{font-size:12px}.share-live small{color:var(--muted-foreground);font-size:9px}.share-live a{width:36px;height:36px;display:grid;place-items:center;border-radius:10px;color:var(--muted-foreground);background:var(--muted)}.share-link-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:9px}.share-link-row input{min-width:0;height:46px;padding:0 13px;border:1px solid var(--input);border-radius:calc(var(--radius)*.62);outline:0;background:var(--background);color:var(--muted-foreground);font-size:10px}.snapshot-status{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:11px;padding:14px;border:1px solid color-mix(in oklch,var(--success) 22%,var(--border));border-radius:calc(var(--radius)*.68);background:color-mix(in oklch,var(--success) 7%,var(--card));color:var(--success-foreground)}.snapshot-status.outdated{border-color:color-mix(in oklch,var(--reminder-foreground) 20%,var(--border));background:color-mix(in oklch,var(--reminder) 55%,var(--card));color:var(--reminder-foreground)}.snapshot-status>div{display:grid;gap:4px}.snapshot-status strong{font-size:10px}.snapshot-status small{color:var(--muted-foreground);font-size:8px}.snapshot-status button{padding:7px 10px;border:1px solid currentColor;border-radius:9px;background:transparent;color:inherit;font-size:9px;font-weight:900}.snapshot-status button:disabled{opacity:.4}.password-form,.create-share{display:grid;gap:16px;padding-top:4px}.password-heading{display:flex;align-items:center;gap:10px}.password-heading>div{display:grid;gap:3px}.password-heading strong{font-size:11px}.password-heading small{color:var(--muted-foreground);font-size:9px}.password-fields{display:grid;grid-template-columns:1fr 1fr;gap:10px}.password-fields label{display:grid;gap:7px}.password-fields span{color:var(--muted-foreground);font-size:9px;font-weight:900;letter-spacing:.06em}.password-fields input{width:100%;height:48px;padding:0 13px;border:1px solid var(--input);border-radius:calc(var(--radius)*.62);outline:0;background:var(--background);font-size:13px;letter-spacing:.14em}.password-fields input:focus{border-color:var(--ring);box-shadow:0 0 0 4px color-mix(in oklch,var(--ring) 12%,transparent)}.password-actions,.share-actions{display:flex;justify-content:flex-end;gap:9px}.share-actions{padding-top:3px;border-top:1px solid var(--border);padding-top:16px}.share-error{margin:0;padding:10px 12px;border-radius:10px;background:color-mix(in oklch,var(--destructive) 8%,transparent);color:var(--destructive);font-size:10px;line-height:1.5}.create-share-button{width:100%}@media(max-width:680px){.mobile-journal-actions{grid-template-columns:repeat(3,minmax(0,1fr))}.mobile-journal-actions .btn{padding:0 9px}.share-intro{grid-template-columns:44px minmax(0,1fr);padding:15px}.share-intro-icon{width:42px;height:42px;border-radius:13px}.password-fields{grid-template-columns:1fr}.snapshot-status{grid-template-columns:auto minmax(0,1fr)}.snapshot-status button{grid-column:1/-1}.share-link-row{grid-template-columns:1fr}.share-link-row .btn{width:100%}}
@media(min-width:681px){.journal-title{align-items:flex-start;gap:24px}.desktop-journal-actions{display:grid;grid-template-columns:repeat(2,minmax(150px,1fr));gap:10px;width:340px}.desktop-journal-actions .btn{width:100%;white-space:nowrap}}
@media(min-width:681px) and (max-width:1120px){.journal-title{flex-direction:column}.journal-actions{justify-content:flex-start}}
</style>
