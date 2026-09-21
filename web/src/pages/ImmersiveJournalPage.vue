<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, BookOpenText, ChevronLeft, ChevronRight, Moon, Sun, Type } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'

interface JournalRecord {
  content?: string
  generatedContent?: string
  generatedAt?: string
}

interface JournalData {
  date: string
  journal: JournalRecord
  entries: unknown[]
}

const route = useRoute()
const router = useRouter()
const data = ref<JournalData | null>(null)
const loading = ref(true)
const error = ref('')
const progress = ref(0)
const night = ref(localStorage.getItem('journal-reader-night') === 'true')
const fontSizes = [17, 19, 21]
const savedFontIndex = Number(localStorage.getItem('journal-reader-font') || 1)
const fontIndex = ref(Number.isInteger(savedFontIndex) && savedFontIndex >= 0 && savedFontIndex < fontSizes.length ? savedFontIndex : 1)

const date = computed(() => String(route.params.date))
const content = computed(() => data.value?.journal.content || data.value?.journal.generatedContent || '')
const dateValue = computed(() => new Date(`${date.value}T12:00:00`))
const year = computed(() => dateValue.value.getFullYear())
const monthDay = computed(() => `${dateValue.value.getMonth() + 1}月${dateValue.value.getDate()}日`)
const weekday = computed(() => new Intl.DateTimeFormat('zh-CN', { weekday: 'long' }).format(dateValue.value))
const traditionalDate = computed(() => {
  const lunarDays = ['', '初一', '初二', '初三', '初四', '初五', '初六', '初七', '初八', '初九', '初十',
    '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十',
    '廿一', '廿二', '廿三', '廿四', '廿五', '廿六', '廿七', '廿八', '廿九', '三十']
  try {
    const parts = new Intl.DateTimeFormat('zh-CN-u-ca-chinese', {
      year: 'numeric', month: 'long', day: 'numeric', timeZone: 'Asia/Shanghai',
    }).formatToParts(dateValue.value)
    const yearName = parts.find(part => String(part.type) === 'yearName')?.value
    const lunarMonth = parts.find(part => part.type === 'month')?.value
    const lunarDay = lunarDays[Number(parts.find(part => part.type === 'day')?.value)]
    return yearName && lunarMonth && lunarDay ? `${yearName}年 · ${lunarMonth}${lunarDay}` : ''
  } catch {
    return ''
  }
})
const readingTime = computed(() => Math.max(1, Math.ceil(content.value.replace(/\s/g, '').length / 400)))
const fontSize = computed(() => fontSizes[Math.min(Math.max(fontIndex.value, 0), fontSizes.length - 1)])

async function load() {
  loading.value = true
  error.value = ''
  window.scrollTo({ top: 0 })
  try {
    data.value = await unwrap<JournalData>(api.get(`/journals/${date.value}`))
  } catch (requestError: any) {
    error.value = requestError.userMessage || '日记加载失败'
  } finally {
    loading.value = false
  }
}

function shiftDate(amount: number) {
  const target = new Date(`${date.value}T12:00:00`)
  target.setDate(target.getDate() + amount)
  router.push({ name: 'journal-reader', params: { date: target.toISOString().slice(0, 10) } })
}

function toggleNight() {
  night.value = !night.value
  localStorage.setItem('journal-reader-night', String(night.value))
}

function cycleFont() {
  fontIndex.value = (fontIndex.value + 1) % fontSizes.length
  localStorage.setItem('journal-reader-font', String(fontIndex.value))
}

function updateProgress() {
  const scrollable = document.documentElement.scrollHeight - window.innerHeight
  progress.value = scrollable <= 0 ? 100 : Math.min(100, Math.round(window.scrollY / scrollable * 100))
}

watch(date, load, { immediate: true })
onMounted(() => window.addEventListener('scroll', updateProgress, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', updateProgress))
</script>

<template>
  <div :class="['reader',{night}]">
    <div class="reading-progress"><i :style="{width:`${progress}%`}" /></div>

    <header class="reader-topbar">
      <button class="round-control back" aria-label="返回日记" @click="router.push({path:'/journal',query:{date}})"><ArrowLeft :size="19" /></button>
      <div class="reader-brand"><BookOpenText :size="15" /><span>RECALLHUB · DAILY PAGES</span></div>
      <div class="top-actions">
        <button class="round-control" aria-label="切换字号" @click="cycleFont"><Type :size="18" /><small>{{fontSize}}</small></button>
        <button class="round-control" :aria-label="night?'切换日间模式':'切换夜间模式'" @click="toggleNight"><Sun v-if="night" :size="18"/><Moon v-else :size="18"/></button>
      </div>
    </header>

    <main class="reader-sheet">
      <Transition name="reader-reveal" mode="out-in">
        <div v-if="loading" key="loading" class="reader-state"><span class="spinner"/>正在翻开这一天…</div>
        <div v-else-if="error" key="error" class="reader-state error-state">{{error}}</div>
        <div v-else key="content" class="reader-content">
          <header class="journal-masthead">
            <div class="date-lockup">
              <span class="year">{{year}}年</span>
              <strong>{{monthDay}}</strong>
              <span class="weekday">{{weekday}}</span>
            </div>
            <div class="reading-meta">
              <span>{{data?.entries.length || 0}} 个原始片段</span>
              <i />
              <span>约 {{readingTime}} 分钟</span>
            </div>
          </header>

          <section v-if="content" class="journal-reading" :style="{fontSize:`${fontSize}px`}">
            <div class="opening-mark">“</div>
            <div class="journal-copy">{{content}}</div>
            <footer class="journal-signoff">
              <div v-if="traditionalDate" class="traditional-calendar">
                <span class="calendar-seal">历</span>
                <span class="calendar-copy"><small>岁次 · 农历</small><strong>{{traditionalDate}}</strong></span>
              </div>
              <div class="written-date"><span>记于</span><strong>{{date}}</strong><i /></div>
            </footer>
          </section>
          <section v-else class="blank-page">
            <BookOpenText :size="30" />
            <h1>这一天还没有整理后的日记</h1>
            <p>返回日记页，可以手动整理或使用 AI 一键整理。</p>
            <button @click="router.push({path:'/journal',query:{date}})">返回整理</button>
          </section>
        </div>
      </Transition>
    </main>

    <footer class="date-navigation">
      <button @click="shiftDate(-1)"><ChevronLeft :size="19"/><span><small>前一天</small>{{new Date(dateValue.getTime()-86400000).toLocaleDateString('zh-CN',{month:'long',day:'numeric'})}}</span></button>
      <div class="page-progress"><i :style="{width:`${progress}%`} "/><span>{{progress}}%</span></div>
      <button class="next" @click="shiftDate(1)"><span><small>后一天</small>{{new Date(dateValue.getTime()+86400000).toLocaleDateString('zh-CN',{month:'long',day:'numeric'})}}</span><ChevronRight :size="19"/></button>
    </footer>
  </div>
</template>

<style scoped>
.reader{--paper:#f7f5ee;--paper-deep:#eeebe1;--ink:#26332f;--soft:#78827c;--rule:rgba(38,51,47,.14);--glow:rgba(61,92,80,.12);min-height:100vh;padding:26px 24px 80px;color:var(--ink);background:radial-gradient(circle at 12% 10%,rgba(191,207,195,.42),transparent 28rem),radial-gradient(circle at 88% 82%,rgba(217,203,176,.32),transparent 30rem),#e8ebe6;transition:background .3s,color .3s}.reader::before{content:"";position:fixed;inset:0;pointer-events:none;opacity:.25;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 160 160' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.75' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.1'/%3E%3C/svg%3E")}.reader.night{--paper:#111820;--paper-deep:#0d131a;--ink:#e8dfd0;--soft:#9d9b96;--rule:rgba(232,223,208,.12);--glow:rgba(205,153,91,.12);background:radial-gradient(circle at 80% 8%,rgba(42,70,98,.42),transparent 32rem),radial-gradient(circle at 14% 86%,rgba(117,72,39,.26),transparent 28rem),#090e14}.reading-progress{position:fixed;z-index:20;left:0;right:0;top:0;height:3px;background:transparent}.reading-progress i{display:block;height:100%;background:#628071;box-shadow:0 0 14px #628071;transition:width .12s}.night .reading-progress i{background:#d6a264;box-shadow:0 0 14px #d6a264}.reader-topbar{position:relative;z-index:2;max-width:920px;margin:0 auto 18px;display:grid;grid-template-columns:1fr auto 1fr;align-items:center}.reader-brand{display:flex;align-items:center;gap:8px;color:var(--soft);font-size:9px;font-weight:900;letter-spacing:.18em}.top-actions{display:flex;justify-content:flex-end;gap:8px}.round-control{width:42px;height:42px;display:flex;align-items:center;justify-content:center;gap:2px;border:1px solid var(--rule);border-radius:50%;background:color-mix(in srgb,var(--paper) 82%,transparent);color:var(--ink);box-shadow:0 9px 30px rgba(20,30,26,.08);backdrop-filter:blur(12px);transition:.2s}.round-control:hover{transform:translateY(-2px);border-color:color-mix(in srgb,var(--ink) 30%,transparent)}.round-control small{font-size:7px;font-weight:900}.reader-topbar .back{justify-self:start}.reader-sheet{position:relative;z-index:1;width:min(860px,100%);min-height:calc(100vh - 190px);margin:auto;padding:72px clamp(34px,8vw,100px) 86px;overflow:hidden;background:var(--paper);border:1px solid var(--rule);border-radius:28px 28px 18px 18px;box-shadow:0 40px 100px rgba(36,49,43,.18),inset 0 1px rgba(255,255,255,.5);transition:background .3s}.reader-sheet::before{content:"";position:absolute;inset:0;pointer-events:none;background:linear-gradient(100deg,rgba(255,255,255,.14),transparent 15%,transparent 86%,rgba(44,38,28,.035)),repeating-linear-gradient(0deg,transparent 0,transparent 4px,rgba(63,55,40,.015) 5px)}.reader-sheet::after{content:"";position:absolute;right:44px;top:0;width:32px;height:76px;background:color-mix(in srgb,#628071 84%,var(--paper));clip-path:polygon(0 0,100% 0,100% 100%,50% 78%,0 100%);opacity:.88}.night .reader-sheet::after{background:#b77b45}.journal-masthead{position:relative;display:flex;align-items:flex-end;justify-content:space-between;gap:28px;padding-bottom:31px;border-bottom:1px solid var(--rule)}.date-lockup{display:grid;grid-template-columns:auto auto;align-items:end;column-gap:14px}.date-lockup .year{grid-column:1/-1;margin-bottom:6px;color:var(--soft);font:500 14px/1 var(--font-display)}.date-lockup strong{font:600 clamp(35px,6vw,54px)/1 var(--font-display);letter-spacing:-.06em}.date-lockup .weekday{padding-bottom:5px;color:var(--soft);font:500 14px/1 var(--font-display)}.reading-meta{display:flex;align-items:center;gap:10px;padding-bottom:5px;color:var(--soft);font-size:10px;font-weight:800;letter-spacing:.06em}.reading-meta i{width:3px;height:3px;border-radius:50%;background:currentColor}.journal-reading{position:relative;padding-top:57px;font-family:var(--font-display);line-height:2.15;letter-spacing:.035em}.opening-mark{position:absolute;top:18px;left:-28px;color:color-mix(in srgb,#628071 16%,transparent);font:700 100px/1 Georgia,serif}.night .opening-mark{color:rgba(214,162,100,.12)}.journal-copy{position:relative;white-space:pre-wrap}.journal-copy::first-letter{float:left;margin:10px 10px 0 0;font-size:3.25em;line-height:.75;color:#47675a}.night .journal-copy::first-letter{color:#d2a16a}.journal-signoff{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-top:58px;padding-top:21px;border-top:1px solid var(--rule);color:var(--soft)}.traditional-calendar{display:flex;align-items:center;gap:11px}.calendar-seal{width:33px;height:33px;display:grid;place-items:center;border:1px solid color-mix(in srgb,#8f5549 72%,transparent);color:#8f5549;font:700 16px/1 var(--font-display);box-shadow:inset 0 0 0 2px var(--paper),inset 0 0 0 3px color-mix(in srgb,#8f5549 28%,transparent)}.night .calendar-seal{border-color:#b77b63;color:#d29a7b;box-shadow:inset 0 0 0 2px var(--paper),inset 0 0 0 3px rgba(210,154,123,.28)}.calendar-copy{display:grid;gap:5px}.calendar-copy small{font:800 7px/1 var(--font-body);letter-spacing:.16em}.calendar-copy strong{color:var(--ink);font:600 12px/1.2 var(--font-display);letter-spacing:.08em}.written-date{display:flex;align-items:center;gap:10px;font:500 10px/1 var(--font-body);letter-spacing:.12em;white-space:nowrap}.written-date strong{font-weight:800;color:var(--ink)}.written-date i{width:38px;height:1px;background:currentColor}.reader-state,.blank-page{min-height:55vh;display:flex;align-items:center;justify-content:center;gap:12px;color:var(--soft);font-size:12px}.error-state{color:#b75a50}.blank-page{flex-direction:column;text-align:center}.blank-page h1{margin:10px 0 0;font:600 28px/1.3 var(--font-display);color:var(--ink)}.blank-page p{margin:0}.blank-page button{margin-top:12px;padding:11px 18px;border:1px solid var(--rule);border-radius:999px;background:var(--ink);color:var(--paper);font-size:11px;font-weight:900}.date-navigation{position:relative;z-index:2;width:min(820px,calc(100% - 20px));margin:18px auto 0;display:grid;grid-template-columns:1fr minmax(120px,.55fr) 1fr;align-items:center;gap:20px}.date-navigation button{display:flex;align-items:center;gap:9px;padding:8px;border:0;background:transparent;color:var(--ink);text-align:left}.date-navigation .next{justify-content:flex-end;text-align:right}.date-navigation span{display:grid;gap:3px;font:600 13px/1.2 var(--font-display)}.date-navigation small{color:var(--soft);font:800 8px/1 var(--font-body);letter-spacing:.12em}.page-progress{display:flex;align-items:center;gap:9px}.page-progress::before{content:"";position:absolute}.page-progress>i{position:relative;display:block;height:2px;max-width:calc(100% - 38px);background:#628071}.page-progress{height:2px;background:var(--rule)}.page-progress span{margin-left:auto;padding-left:6px;color:var(--soft);background:transparent;font:800 8px/1 var(--font-body)}.night .page-progress>i{background:#d6a264}@media(max-width:680px){.reader{padding:14px 0 64px}.reader-topbar{padding:0 15px;margin-bottom:12px}.reader-brand span{display:none}.reader-sheet{min-height:calc(100vh - 140px);padding:54px 27px 66px;border-left:0;border-right:0;border-radius:0}.reader-sheet::after{right:24px;width:26px;height:62px}.journal-masthead{align-items:flex-start;flex-direction:column;gap:16px}.reading-meta{padding:0}.journal-reading{padding-top:45px;line-height:2}.opening-mark{left:-12px}.journal-signoff{align-items:stretch;flex-direction:column;gap:18px}.written-date{align-self:flex-end}.date-navigation{grid-template-columns:1fr 1fr;width:calc(100% - 20px)}.page-progress{position:fixed;left:0;right:0;bottom:0;height:3px;z-index:10}.page-progress span{display:none}}
.reader-content{position:relative}.reader-reveal-leave-active{transition:opacity .18s ease}.reader-reveal-enter-active{transition:opacity .38s ease,transform .38s cubic-bezier(.2,.8,.2,1)}.reader-reveal-leave-to{opacity:0}.reader-reveal-enter-from{opacity:0;transform:translateY(4px)}.reader-reveal-enter-active .journal-copy{animation:reader-copy-in .48s .04s ease-out both}@keyframes reader-copy-in{from{opacity:0;transform:translateY(3px)}to{opacity:1;transform:none}}
</style>
