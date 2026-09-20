<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bell, RefreshCw } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import type { Entry, TodayData } from '@/types'
import QuickCapture from '@/components/QuickCapture.vue'
import EntryCard from '@/components/EntryCard.vue'
import AppModal from '@/components/AppModal.vue'
import EntryEditor from '@/components/EntryEditor.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const data=ref<TodayData|null>(null),loading=ref(true),editing=ref<Entry|null>(null)
const deleting=ref<Entry|null>(null)
const dateTitle=computed(()=>data.value?new Intl.DateTimeFormat('zh-CN',{month:'long',day:'numeric',weekday:'long'}).format(new Date(data.value.date+'T12:00:00')):'今天')
const dayNumber=computed(()=>data.value?new Date(data.value.date+'T12:00:00').getDate():'')
const journalText=computed(()=>String(data.value?.journal?.content||data.value?.journal?.generatedContent||'今天还没有整理后的日记。'))
async function load(){loading.value=true;try{data.value=await unwrap<TodayData>(api.get('/today'))}finally{loading.value=false}}
async function complete(e:Entry){if(e.status!=='DONE')await api.post(`/entries/${e.id}/complete`);load()}
async function remove(e:Entry){deleting.value=e}
async function confirmRemove(){if(!deleting.value)return;await api.delete(`/entries/${deleting.value.id}`);deleting.value=null;load()}
const entryName=(id:number)=>[...(data.value?.overdue||[]),...(data.value?.todos||[]),...(data.value?.entries||[])].find(e=>e.id===id)?.title||'一条记录'
const retryText=(next?:string)=>next?`${new Date(next).toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'})} 重试`:'等待重试'
onMounted(load)
</script>

<template>
  <div class="page today-page">
    <section class="today-hero">
      <span class="day-orbit">{{dayNumber}}</span>
      <header class="page-header"><div><p class="eyebrow">TODAY'S TRACE</p><h1 class="page-title">{{dateTitle}}</h1><p class="page-subtitle">把今天放在这里，明天就不会忘记。</p></div><button class="icon-btn refresh" aria-label="刷新" @click="load"><RefreshCw :size="18"/></button></header>
      <div v-if="data" class="daily-pulse"><div><strong>{{data.todos.length}}</strong><span>ACTIVE TODO</span></div><div><strong>{{data.entries.length}}</strong><span>NEW MEMORIES</span></div><div><strong>{{data.reminders.length}}</strong><span>REMINDERS</span></div></div>
    </section>
    <QuickCapture @saved="load"/>
    <div v-if="loading" class="loading"><span class="spinner"/></div>
    <template v-else-if="data">
      <div class="daily-grid">
        <main class="today-main">
          <section class="section"><div class="section-head"><h2 class="section-title">待办清单</h2><span class="count">{{data.todos.length}} 件待确认完成</span></div><div v-if="!data.todos.length" class="empty">今天没有待办压在心上。</div><EntryCard v-for="e in data.todos" :key="e.id" :entry="e" @complete="complete" @remove="remove" @open="editing=$event"/></section>
          <section class="section"><div class="section-head"><h2 class="section-title">今天记录</h2><span class="count">{{data.entries.length}} 条新记忆</span></div><div v-if="!data.entries.length" class="empty">今天还没有留下文字。</div><EntryCard v-for="e in data.entries" :key="e.id" :entry="e" show-time @remove="remove" @open="editing=$event" @complete="complete"/></section>
        </main>
        <aside class="today-aside">
          <section class="side-panel reminder-panel"><div class="section-head"><h2 class="section-title">今天提醒</h2><span class="count">{{data.reminders.length}}</span></div><div v-if="data.reminders.length" class="reminder-strip"><div v-for="r in data.reminders" :key="r.id" class="reminder-item" :class="{retrying:r.lastError}"><span class="reminder-time">{{new Date(r.remindAt).toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'})}}</span><span class="bell"><Bell :size="14"/></span><div class="reminder-copy"><strong>{{entryName(r.entryId)}}</strong><small v-if="r.lastError">投递失败 · {{retryText(r.nextAttemptAt)}}</small></div></div></div><div v-else class="side-empty">今天没有定时提醒</div></section>
          <section v-if="data.overdue.length" class="side-panel overdue-section"><div class="section-head"><h2 class="section-title">已经超时</h2><span class="count">{{data.overdue.length}}</span></div><EntryCard v-for="e in data.overdue" :key="e.id" :entry="e" compact @complete="complete" @remove="remove" @open="editing=$event"/></section>
          <section class="journal-glance"><span>DAILY JOURNAL</span><blockquote>“{{journalText}}”</blockquote><i>— {{dateTitle}}</i></section>
        </aside>
      </div>
    </template>
    <AppModal v-if="editing" title="编辑记录" @close="editing=null"><EntryEditor :entry="editing" @saved="editing=null;load()"/></AppModal>
    <ConfirmDialog v-if="deleting" title="删除这条记录？" message="删除后不会继续出现在今天和时间线中。" confirm-text="删除" @cancel="deleting=null" @confirm="confirmRemove"/>
  </div>
</template>

<style scoped>
.today-page{max-width:1400px}.today-hero{position:relative;min-height:205px;display:flex;align-items:flex-end;margin:-10px 0 22px;padding:35px 36px 31px;overflow:hidden;border:1px solid color-mix(in oklch,var(--primary) 16%,var(--border));border-radius:var(--radius);background:linear-gradient(120deg,color-mix(in oklch,var(--card) 92%,transparent),color-mix(in oklch,var(--primary) 8%,var(--card)));box-shadow:0 28px 80px color-mix(in oklch,var(--primary) 8%,transparent)}.today-hero::after{content:"";position:absolute;width:280px;height:280px;right:-78px;top:-118px;border:52px solid color-mix(in oklch,var(--primary) 8%,transparent);border-radius:50%}.today-hero .page-header{z-index:1;flex:1;margin:0}.day-orbit{position:absolute;right:56px;top:16px;font-family:var(--font-display);font-size:145px;font-weight:700;line-height:1;color:color-mix(in oklch,var(--primary) 9%,transparent);letter-spacing:-.08em}.refresh{align-self:flex-start;background:color-mix(in oklch,var(--card) 58%,transparent);border-color:var(--border)}.daily-pulse{position:relative;z-index:1;display:grid;grid-template-columns:repeat(3,1fr);align-self:flex-end;margin:0 22px 3px 40px;border-left:1px solid var(--border)}.daily-pulse div{min-width:92px;padding:4px 18px}.daily-pulse strong{display:block;font-family:var(--font-display);font-size:27px;line-height:1}.daily-pulse span{display:block;margin-top:7px;color:var(--muted-foreground);font-size:7px;font-weight:900;letter-spacing:.12em;white-space:nowrap}.loading{display:grid;place-items:center;padding:100px}.daily-grid{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(290px,.72fr);gap:30px;align-items:start}.today-main .section:first-child{margin-top:34px}.today-aside{position:sticky;top:28px;margin-top:34px;display:grid;gap:17px}.side-panel{padding:20px;border:1px solid var(--border);border-radius:var(--radius);background:color-mix(in oklch,var(--card) 86%,transparent);box-shadow:0 16px 46px color-mix(in oklch,var(--foreground) 5%,transparent)}.reminder-panel{background:linear-gradient(145deg,color-mix(in oklch,var(--reminder) 35%,var(--card)),color-mix(in oklch,var(--card) 90%,transparent))}.reminder-strip{padding-top:2px}.reminder-item{display:grid;grid-template-columns:49px 30px 1fr;align-items:center;min-height:57px;border-bottom:1px solid color-mix(in oklch,var(--border) 78%,transparent);font-size:11px}.reminder-item:last-child{border:0}.reminder-time{font-variant-numeric:tabular-nums;color:var(--reminder-foreground);font-weight:900}.bell{width:27px;height:27px;display:grid;place-items:center;border-radius:calc(var(--radius)*.45);background:var(--reminder);color:var(--reminder-foreground)}.side-empty{display:grid;place-items:center;min-height:82px;color:var(--muted-foreground);font-size:11px;border:1px dashed color-mix(in oklch,var(--border) 80%,transparent);border-radius:calc(var(--radius)*.7)}.overdue-section{background:color-mix(in oklch,var(--overdue) 20%,var(--card));border-color:color-mix(in oklch,var(--overdue-foreground) 15%,var(--border))}.journal-glance{position:relative;padding:24px;overflow:hidden;border-radius:var(--radius);background:var(--foreground);color:var(--background);box-shadow:0 18px 50px color-mix(in oklch,var(--foreground) 18%,transparent)}.journal-glance::after{content:"“";position:absolute;right:14px;top:-28px;font-family:var(--font-display);font-size:120px;line-height:1;color:color-mix(in oklch,var(--background) 8%,transparent)}.journal-glance>span{position:relative;z-index:1;color:color-mix(in oklch,var(--background) 62%,transparent);font-size:8px;font-weight:900;letter-spacing:.18em}.journal-glance blockquote{position:relative;z-index:1;margin:17px 0 16px;font-family:var(--font-display);font-size:14px;line-height:1.9}.journal-glance i{font-size:9px;font-style:normal;color:color-mix(in oklch,var(--background) 55%,transparent)}@media(max-width:1120px){.daily-pulse{display:none}.daily-grid{grid-template-columns:minmax(0,1.5fr) minmax(260px,.75fr)}}@media(max-width:880px){.daily-grid{grid-template-columns:1fr}.today-aside{position:static;grid-template-columns:1fr 1fr}.journal-glance{grid-column:1/-1}}@media(max-width:760px){.today-hero{min-height:180px;margin:-4px 0 15px;padding:26px 19px 23px}.day-orbit{right:22px;top:32px;font-size:105px}.today-hero .page-header{padding-left:14px}.today-aside{grid-template-columns:1fr}.journal-glance{grid-column:auto}.daily-grid{gap:0}.today-main .section:first-child{margin-top:28px}}
@media(max-width:760px){.today-hero .page-title{font-size:34px;letter-spacing:-.065em}}
.reminder-copy{min-width:0;display:grid;gap:4px}.reminder-copy strong{font-size:11px}.reminder-copy small{color:var(--destructive);font-size:9px;font-weight:800}.reminder-item.retrying .bell{background:color-mix(in oklch,var(--destructive) 12%,var(--reminder));color:var(--destructive)}
</style>
