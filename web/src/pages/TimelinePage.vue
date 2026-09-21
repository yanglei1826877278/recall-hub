<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Filter, LoaderCircle } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import type { Entry, EntryType } from '@/types'
import EntryCard from '@/components/EntryCard.vue'
import AppModal from '@/components/AppModal.vue'
import EntryEditor from '@/components/EntryEditor.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

interface Page {items:Entry[];nextCursor?:string;hasMore:boolean}
const entries=ref<Entry[]>([]),type=ref<EntryType|''>(''),cursor=ref<string>(),more=ref(false),loading=ref(false),editing=ref<Entry|null>(null)
const deleting=ref<Entry|null>(null)
const groups=computed(()=>{const map=new Map<string,Entry[]>();for(const e of entries.value){const key=new Intl.DateTimeFormat('zh-CN',{year:'numeric',month:'long',day:'numeric'}).format(new Date(e.occurredAt||e.createdAt));map.set(key,[...(map.get(key)||[]),e])}return [...map.entries()]})
async function load(reset=false){loading.value=true;try{const p=await unwrap<Page>(api.get('/timeline',{params:{type:type.value||undefined,before:reset?undefined:cursor.value,pageSize:30}}));entries.value=reset?p.items:[...entries.value,...p.items];cursor.value=p.nextCursor;more.value=p.hasMore}finally{loading.value=false}}
async function remove(e:Entry){deleting.value=e}
async function confirmRemove(){if(!deleting.value)return;await api.delete(`/entries/${deleting.value.id}`);deleting.value=null;load(true)}
async function complete(e:Entry){await api.post(`/entries/${e.id}/complete`);load(true)}
onMounted(()=>load(true))
</script>

<template>
  <div class="page timeline-page">
    <header class="page-header"><div><p class="eyebrow">MEMORY STREAM</p><h1 class="page-title">时间线</h1><p class="page-subtitle">最近发生的事，按时间缓缓流过。</p></div></header>
    <div class="filters"><Filter :size="15"/><button v-for="f in [{v:'',l:'全部'},{v:'DIARY',l:'日记'},{v:'TODO',l:'待办'},{v:'IDEA',l:'想法'},{v:'NOTE',l:'备忘'}]" :key="f.v" :class="{active:type===f.v}" @click="type=f.v as any;load(true)">{{f.l}}</button></div>
    <div v-if="!groups.length&&!loading" class="empty">时间线上还很安静。</div>
    <section v-for="[date,items] in groups" :key="date" class="day-group"><header><h2>{{date}}</h2><span>{{items.length}} 条</span></header><div class="rail"><div v-for="e in items" :key="e.id" class="rail-row"><time>{{new Date(e.occurredAt||e.createdAt).toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'})}}</time><span class="dot" :class="`dot-${e.type}`"/><EntryCard :entry="e" compact @open="editing=$event" @remove="remove" @complete="complete"/></div></div></section>
    <button v-if="more" class="btn btn-secondary load-more" :disabled="loading" @click="load()"><LoaderCircle v-if="loading" :size="16" class="spin"/>加载更早的记录</button>
    <AppModal v-if="editing" title="编辑记录" @close="editing=null"><EntryEditor :entry="editing" @saved="editing=null;load(true)"/></AppModal>
    <ConfirmDialog v-if="deleting" title="删除这条记录？" message="删除后不会继续出现在时间线和搜索结果中。" confirm-text="删除" @cancel="deleting=null" @confirm="confirmRemove"/>
  </div>
</template>

<style scoped>
.timeline-page{max-width:1220px}.filters{position:sticky;top:18px;z-index:12;display:flex;align-items:center;gap:4px;width:max-content;max-width:100%;padding:6px;margin:-4px 0 42px;border:1px solid color-mix(in oklch,var(--border) 84%,transparent);border-radius:999px;background:color-mix(in oklch,var(--card) 88%,transparent);box-shadow:0 10px 34px color-mix(in oklch,var(--foreground) 6%,transparent);backdrop-filter:blur(18px);color:var(--muted-foreground)}.filters>svg{margin:0 6px 0 7px}.filters button{border:0;background:transparent;border-radius:999px;padding:9px 14px;font-size:9px;font-weight:900;letter-spacing:.05em;color:inherit;transition:.18s}.filters button:hover{color:var(--foreground);background:var(--muted)}.filters button.active{background:var(--primary);color:var(--primary-foreground);box-shadow:0 7px 18px color-mix(in oklch,var(--primary) 24%,transparent)}.day-group{position:relative;margin:0 0 54px;padding:0 0 0 2px}.day-group>header{display:flex;align-items:center;gap:20px;margin-bottom:20px}.day-group>header::after{content:"";height:1px;flex:1;background:linear-gradient(90deg,var(--border),transparent)}.day-group h2{font:650 27px/1.2 var(--font-display);letter-spacing:-.04em;margin:0}.day-group header span{order:3;font-size:9px;font-weight:900;letter-spacing:.08em;color:var(--muted-foreground)}.rail{position:relative}.rail::before{content:"";position:absolute;left:76px;top:14px;bottom:14px;width:1px;background:linear-gradient(var(--primary),var(--border) 20%,var(--border) 80%,transparent)}.rail-row{position:relative;display:grid;grid-template-columns:57px 39px minmax(0,1fr);align-items:start;margin-bottom:12px}.rail-row time{padding-top:17px;font-size:9px;font-weight:800;color:var(--muted-foreground);font-variant-numeric:tabular-nums}.dot{z-index:1;justify-self:center;margin-top:18px;width:11px;height:11px;border:3px solid var(--background);border-radius:99px;box-shadow:0 0 0 1px var(--border),0 0 0 5px color-mix(in oklch,var(--background) 80%,transparent)}.dot-TODO{background:var(--todo-foreground)}.dot-DIARY{background:var(--diary-foreground)}.dot-IDEA{background:var(--idea-foreground)}.dot-NOTE{background:var(--note-foreground)}.rail-row :deep(.entry-card){margin:0}.load-more{display:flex;margin:30px auto}.spin{animation:spin .7s linear infinite}@media(max-width:600px){.filters{overflow:auto;top:8px;margin-bottom:32px}.filters button{padding:8px 11px;white-space:nowrap}.day-group{padding-left:0}.day-group h2{font-size:22px}.rail::before{left:32px}.rail-row{grid-template-columns:24px 16px minmax(0,1fr)}.rail-row time{padding-top:18px;font-size:7px;letter-spacing:-.035em;transform:none}.dot{margin-top:19px;width:9px;height:9px;border-width:2px;box-shadow:0 0 0 1px var(--border),0 0 0 3px color-mix(in oklch,var(--background) 84%,transparent)}}
.filters button{padding:10px 15px;font-size:12px;letter-spacing:.02em}
</style>
