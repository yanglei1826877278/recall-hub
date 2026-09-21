<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { BookOpenText, KeyRound, LoaderCircle, Moon, ShieldCheck, Sun, Type } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'

interface PublicShareState {
  unlocked: boolean
  passwordDigits: number
  date?: string
  content?: string
}

const route = useRoute()
const token = computed(() => String(route.params.token || ''))
const state = ref<PublicShareState | null>(null)
const loading = ref(true)
const unlocking = ref(false)
const pageError = ref('')
const passwordError = ref('')
const password = ref('')
const passwordInput = ref<HTMLInputElement | null>(null)
const progress = ref(0)
const night = ref(localStorage.getItem('journal-reader-night') === 'true')
const fontSizes = [17, 19, 21]
const savedFontIndex = Number(localStorage.getItem('journal-reader-font') || 1)
const fontIndex = ref(Number.isInteger(savedFontIndex) && savedFontIndex >= 0 && savedFontIndex < fontSizes.length ? savedFontIndex : 1)
let previousRobots: string | null = null
let accessTimer: number | undefined

const unlocked = computed(() => Boolean(state.value?.unlocked && state.value.date && state.value.content))
const date = computed(() => state.value?.date || '')
const content = computed(() => state.value?.content || '')
const dateValue = computed(() => new Date(`${date.value || '2000-01-01'}T12:00:00`))
const year = computed(() => dateValue.value.getFullYear())
const monthDay = computed(() => `${dateValue.value.getMonth() + 1}月${dateValue.value.getDate()}日`)
const weekday = computed(() => date.value ? new Intl.DateTimeFormat('zh-CN', { weekday: 'long' }).format(dateValue.value) : '')
const readingTime = computed(() => Math.max(1, Math.ceil(content.value.replace(/\s/g, '').length / 400)))
const fontSize = computed(() => fontSizes[fontIndex.value])
const passwordDigits = computed(() => state.value?.passwordDigits === 6 ? 6 : 4)
const traditionalDate = computed(() => {
  if (!date.value) return ''
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

async function load() {
  loading.value = true
  pageError.value = ''
  try {
    state.value = await unwrap<PublicShareState>(api.get(`/public/journal-shares/${encodeURIComponent(token.value)}`))
    if (!state.value.unlocked) await nextTick(() => passwordInput.value?.focus())
  } catch (error: any) {
    pageError.value = error.userMessage || '这页日记暂时无法打开'
  } finally {
    loading.value = false
  }
}

async function refreshAccess() {
  if (!unlocked.value || document.visibilityState !== 'visible') return
  try {
    const latest = await unwrap<PublicShareState>(api.get(`/public/journal-shares/${encodeURIComponent(token.value)}`))
    state.value = latest
    if (!latest.unlocked) {
      passwordError.value = '分享密码已经更新，请重新输入。'
      await nextTick(() => passwordInput.value?.focus())
    }
  } catch (error: any) {
    state.value = null
    pageError.value = error.userMessage || '这页日记已经无法查看'
  }
}

function normalizePassword() {
  password.value = password.value.replace(/\D/g, '').slice(0, passwordDigits.value)
  passwordError.value = ''
}

async function unlock() {
  if (!/^\d+$/.test(password.value) || password.value.length !== passwordDigits.value || unlocking.value) {
    passwordError.value = `请输入 ${passwordDigits.value} 位数字查看密码`
    return
  }
  unlocking.value = true
  passwordError.value = ''
  try {
    state.value = await unwrap<PublicShareState>(api.post(
      `/public/journal-shares/${encodeURIComponent(token.value)}/unlock`,
      { password: password.value },
    ))
    password.value = ''
    window.scrollTo({ top: 0 })
  } catch (error: any) {
    passwordError.value = error.userMessage || '查看密码错误'
    password.value = ''
    await nextTick(() => passwordInput.value?.focus())
  } finally {
    unlocking.value = false
  }
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
  if (!unlocked.value) { progress.value = 0; return }
  const scrollable = document.documentElement.scrollHeight - window.innerHeight
  progress.value = scrollable <= 0 ? 100 : Math.min(100, Math.round(window.scrollY / scrollable * 100))
}

onMounted(() => {
  const meta = document.querySelector<HTMLMetaElement>('meta[name="robots"]')
  previousRobots = meta?.content || null
  const robots = meta || Object.assign(document.createElement('meta'), { name: 'robots' })
  robots.content = 'noindex,nofollow,noarchive'
  if (!meta) document.head.appendChild(robots)
  window.addEventListener('scroll', updateProgress, { passive: true })
  window.addEventListener('focus', refreshAccess)
  accessTimer = window.setInterval(refreshAccess, 30_000)
  load()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateProgress)
  window.removeEventListener('focus', refreshAccess)
  if (accessTimer !== undefined) window.clearInterval(accessTimer)
  const meta = document.querySelector<HTMLMetaElement>('meta[name="robots"]')
  if (meta) {
    if (previousRobots == null) meta.remove()
    else meta.content = previousRobots
  }
})
</script>

<template>
  <div :class="['shared-reader', { night }]">
    <div v-if="unlocked" class="reading-progress"><i :style="{ width: `${progress}%` }" /></div>

    <header class="reader-topbar">
      <div class="privacy-mark"><ShieldCheck :size="15" /><span>受密码保护的单日日记</span></div>
      <div class="reader-brand"><BookOpenText :size="15" /><span>RECALLHUB · SHARED PAGE</span></div>
      <div class="top-actions">
        <button v-if="unlocked" class="round-control" aria-label="切换字号" @click="cycleFont"><Type :size="18" /><small>{{ fontSize }}</small></button>
        <button class="round-control" :aria-label="night ? '切换日间模式' : '切换夜间模式'" @click="toggleNight"><Sun v-if="night" :size="18"/><Moon v-else :size="18"/></button>
      </div>
    </header>

    <main class="reader-sheet">
      <Transition name="reader-reveal" mode="out-in">
        <div v-if="loading" key="loading" class="reader-state"><LoaderCircle class="spin" :size="22" />正在翻开这页日记…</div>
        <div v-else-if="pageError" key="error" class="reader-state unavailable">
          <BookOpenText :size="31" />
          <h1>这一页已经合上了</h1>
          <p>{{ pageError }}</p>
        </div>

        <section v-else-if="!unlocked" key="unlock" class="unlock-page">
          <div class="lock-seal"><KeyRound :size="27" /></div>
          <p class="kicker">PRIVATE DAILY PAGE</p>
          <h1>打开这一天</h1>
          <p class="unlock-copy">分享者为这页日记设置了查看密码。</p>
          <form class="unlock-form" @submit.prevent="unlock">
            <label for="share-password">{{ passwordDigits }} 位查看密码</label>
            <input
              id="share-password"
              ref="passwordInput"
              v-model="password"
              class="password-input"
              type="password"
              inputmode="numeric"
              autocomplete="current-password"
              :maxlength="passwordDigits"
              :pattern="`[0-9]{${passwordDigits}}`"
              aria-describedby="password-help"
              @input="normalizePassword"
            />
            <p id="password-help" :class="['password-help', { error: passwordError }]">{{ passwordError || '输入密码后，日记会在这一页展开' }}</p>
            <button :disabled="unlocking || password.length !== passwordDigits">
              <LoaderCircle v-if="unlocking" class="spin" :size="17" />
              <KeyRound v-else :size="17" />
              {{ unlocking ? '正在打开…' : '打开日记' }}
            </button>
          </form>
          <div class="privacy-note"><ShieldCheck :size="14" />这里只展示分享时保存的正文快照</div>
        </section>

        <div v-else key="content" class="reader-content">
          <header class="journal-masthead">
            <div class="date-lockup">
              <span class="year">{{ year }}年</span>
              <strong>{{ monthDay }}</strong>
              <span class="weekday">{{ weekday }}</span>
            </div>
            <div class="reading-meta"><span>分享的一页</span><i/><span>约 {{ readingTime }} 分钟</span></div>
          </header>

          <section class="journal-reading" :style="{ fontSize: `${fontSize}px` }">
            <div class="opening-mark">“</div>
            <div class="journal-copy">{{ content }}</div>
            <footer class="journal-signoff">
              <div v-if="traditionalDate" class="traditional-calendar">
                <span class="calendar-seal">历</span>
                <span class="calendar-copy"><small>岁次 · 农历</small><strong>{{ traditionalDate }}</strong></span>
              </div>
              <div class="written-date"><span>记于</span><strong>{{ date }}</strong><i/></div>
            </footer>
          </section>
        </div>
      </Transition>
    </main>

    <footer class="share-footer"><ShieldCheck :size="13" />由 RecallHub 私密分享 · 每次只打开一天</footer>
  </div>
</template>

<style scoped>
.shared-reader{--paper:#f7f5ee;--ink:#26332f;--soft:#78827c;--rule:rgba(38,51,47,.14);min-height:100vh;padding:26px 24px 48px;color:var(--ink);background:radial-gradient(circle at 12% 10%,rgba(191,207,195,.42),transparent 28rem),radial-gradient(circle at 88% 82%,rgba(217,203,176,.32),transparent 30rem),#e8ebe6;transition:background .3s,color .3s}.shared-reader::before{content:"";position:fixed;inset:0;pointer-events:none;opacity:.25;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 160 160' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.75' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.1'/%3E%3C/svg%3E")}.shared-reader.night{--paper:#111820;--ink:#e8dfd0;--soft:#9d9b96;--rule:rgba(232,223,208,.12);background:radial-gradient(circle at 80% 8%,rgba(42,70,98,.42),transparent 32rem),radial-gradient(circle at 14% 86%,rgba(117,72,39,.26),transparent 28rem),#090e14}.reading-progress{position:fixed;z-index:20;left:0;right:0;top:0;height:3px}.reading-progress i{display:block;height:100%;background:#628071;box-shadow:0 0 14px #628071;transition:width .12s}.night .reading-progress i{background:#d6a264;box-shadow:0 0 14px #d6a264}.reader-topbar{position:relative;z-index:2;max-width:920px;margin:0 auto 18px;display:grid;grid-template-columns:1fr auto 1fr;align-items:center}.privacy-mark,.reader-brand{display:flex;align-items:center;gap:8px;color:var(--soft);font-size:9px;font-weight:900;letter-spacing:.12em}.reader-brand{letter-spacing:.18em}.top-actions{display:flex;justify-content:flex-end;gap:8px}.round-control{width:42px;height:42px;display:flex;align-items:center;justify-content:center;gap:2px;border:1px solid var(--rule);border-radius:50%;background:color-mix(in srgb,var(--paper) 82%,transparent);color:var(--ink);box-shadow:0 9px 30px rgba(20,30,26,.08);backdrop-filter:blur(12px);transition:.2s}.round-control:hover{transform:translateY(-2px)}.round-control small{font-size:7px;font-weight:900}.reader-sheet{position:relative;z-index:1;width:min(860px,100%);min-height:calc(100vh - 170px);margin:auto;padding:72px clamp(34px,8vw,100px) 86px;overflow:hidden;background:var(--paper);border:1px solid var(--rule);border-radius:28px 28px 18px 18px;box-shadow:0 40px 100px rgba(36,49,43,.18),inset 0 1px rgba(255,255,255,.5);transition:background .3s}.reader-sheet::before{content:"";position:absolute;inset:0;pointer-events:none;background:linear-gradient(100deg,rgba(255,255,255,.14),transparent 15%,transparent 86%,rgba(44,38,28,.035)),repeating-linear-gradient(0deg,transparent 0,transparent 4px,rgba(63,55,40,.015) 5px)}.reader-sheet::after{content:"";position:absolute;right:44px;top:0;width:32px;height:76px;background:color-mix(in srgb,#628071 84%,var(--paper));clip-path:polygon(0 0,100% 0,100% 100%,50% 78%,0 100%);opacity:.88}.night .reader-sheet::after{background:#b77b45}.reader-state{position:relative;min-height:55vh;display:flex;align-items:center;justify-content:center;gap:12px;color:var(--soft);font-size:12px}.unavailable{flex-direction:column;text-align:center}.unavailable h1{margin:10px 0 0;color:var(--ink);font:600 28px/1.3 var(--font-display)}.unavailable p{margin:0}.unlock-page{position:relative;z-index:1;min-height:55vh;display:flex;align-items:center;flex-direction:column;justify-content:center;text-align:center}.lock-seal{width:66px;height:66px;display:grid;place-items:center;margin-bottom:23px;border:1px solid color-mix(in srgb,#628071 35%,var(--rule));border-radius:50%;color:#567565;background:color-mix(in srgb,#c6d3c9 28%,transparent);box-shadow:0 0 0 9px color-mix(in srgb,#628071 5%,transparent)}.night .lock-seal{color:#d6a264;border-color:rgba(214,162,100,.3);background:rgba(214,162,100,.08);box-shadow:0 0 0 9px rgba(214,162,100,.035)}.kicker{margin:0 0 9px;color:var(--soft);font-size:8px;font-weight:900;letter-spacing:.24em}.unlock-page h1{margin:0;font:600 clamp(34px,5vw,48px)/1.2 var(--font-display);letter-spacing:-.055em}.unlock-copy{margin:12px 0 28px;color:var(--soft);font-size:12px}.unlock-form{width:min(340px,100%)}.unlock-form label{display:block;margin-bottom:10px;color:var(--soft);font-size:9px;font-weight:900;letter-spacing:.12em}.password-input{width:100%;height:62px;padding:0 15px 0 30px;border:1px solid var(--rule);border-radius:16px;outline:none;background:color-mix(in srgb,var(--paper) 75%,transparent);color:var(--ink);font:600 25px/1 var(--font-body);text-align:center;letter-spacing:.65em;box-shadow:inset 0 1px rgba(255,255,255,.4);transition:.2s}.password-input:focus{border-color:#628071;box-shadow:0 0 0 5px color-mix(in srgb,#628071 11%,transparent)}.password-help{min-height:18px;margin:8px 0 13px;color:var(--soft);font-size:9px}.password-help.error{color:#ad514a}.unlock-form button{width:100%;height:49px;display:flex;align-items:center;justify-content:center;gap:8px;border:0;border-radius:14px;background:var(--ink);color:var(--paper);font-size:11px;font-weight:900;box-shadow:0 12px 28px rgba(38,51,47,.16);transition:.2s}.unlock-form button:hover:not(:disabled){transform:translateY(-2px)}.unlock-form button:disabled{opacity:.45}.privacy-note{display:flex;align-items:center;gap:6px;margin-top:21px;color:var(--soft);font-size:9px}.journal-masthead{position:relative;display:flex;align-items:flex-end;justify-content:space-between;gap:28px;padding-bottom:31px;border-bottom:1px solid var(--rule)}.date-lockup{display:grid;grid-template-columns:auto auto;align-items:end;column-gap:14px}.date-lockup .year{grid-column:1/-1;margin-bottom:6px;color:var(--soft);font:500 14px/1 var(--font-display)}.date-lockup strong{font:600 clamp(35px,6vw,54px)/1 var(--font-display);letter-spacing:-.06em}.date-lockup .weekday{padding-bottom:5px;color:var(--soft);font:500 14px/1 var(--font-display)}.reading-meta{display:flex;align-items:center;gap:10px;padding-bottom:5px;color:var(--soft);font-size:10px;font-weight:800;letter-spacing:.06em}.reading-meta i{width:3px;height:3px;border-radius:50%;background:currentColor}.journal-reading{position:relative;padding-top:57px;font-family:var(--font-display);line-height:2.15;letter-spacing:.035em}.opening-mark{position:absolute;top:18px;left:-28px;color:color-mix(in srgb,#628071 16%,transparent);font:700 100px/1 Georgia,serif}.night .opening-mark{color:rgba(214,162,100,.12)}.journal-copy{position:relative;white-space:pre-wrap}.journal-copy::first-letter{float:left;margin:10px 10px 0 0;font-size:3.25em;line-height:.75;color:#47675a}.night .journal-copy::first-letter{color:#d2a16a}.journal-signoff{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-top:58px;padding-top:21px;border-top:1px solid var(--rule);color:var(--soft)}.traditional-calendar{display:flex;align-items:center;gap:11px}.calendar-seal{width:33px;height:33px;display:grid;place-items:center;border:1px solid color-mix(in srgb,#8f5549 72%,transparent);color:#8f5549;font:700 16px/1 var(--font-display);box-shadow:inset 0 0 0 2px var(--paper),inset 0 0 0 3px color-mix(in srgb,#8f5549 28%,transparent)}.calendar-copy{display:grid;gap:5px}.calendar-copy small{font:800 7px/1 var(--font-body);letter-spacing:.16em}.calendar-copy strong{color:var(--ink);font:600 12px/1.2 var(--font-display);letter-spacing:.08em}.written-date{display:flex;align-items:center;gap:10px;font:500 10px/1 var(--font-body);letter-spacing:.12em;white-space:nowrap}.written-date strong{font-weight:800;color:var(--ink)}.written-date i{width:38px;height:1px;background:currentColor}.share-footer{position:relative;z-index:1;display:flex;align-items:center;justify-content:center;gap:6px;margin:19px auto 0;color:var(--soft);font-size:8px;font-weight:800;letter-spacing:.08em}.spin{animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:680px){.shared-reader{padding:14px 0 30px}.reader-topbar{grid-template-columns:1fr auto;padding:0 15px;margin-bottom:12px}.privacy-mark{display:none}.reader-brand{justify-self:start}.reader-brand span{display:none}.reader-sheet{min-height:calc(100vh - 125px);padding:54px 27px 66px;border-left:0;border-right:0;border-radius:0}.reader-sheet::after{right:24px;width:26px;height:62px}.journal-masthead{align-items:flex-start;flex-direction:column;gap:16px}.reading-meta{padding:0}.journal-reading{padding-top:45px;line-height:2}.opening-mark{left:-12px}.journal-signoff{align-items:stretch;flex-direction:column;gap:18px}.written-date{align-self:flex-end}.share-footer{padding:0 16px}}
.reader-content{position:relative}.reader-reveal-leave-active{transition:opacity .18s ease}.reader-reveal-enter-active{transition:opacity .38s ease,transform .38s cubic-bezier(.2,.8,.2,1)}.reader-reveal-leave-to{opacity:0}.reader-reveal-enter-from{opacity:0;transform:translateY(4px)}.reader-reveal-enter-active .journal-copy{animation:reader-copy-in .48s .04s ease-out both}@keyframes reader-copy-in{from{opacity:0;transform:translateY(3px)}to{opacity:1;transform:none}}
</style>
