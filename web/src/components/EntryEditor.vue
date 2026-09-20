<script setup lang="ts">
import { ref } from 'vue'
import { Bell, CircleCheckBig, Save } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import type { Entry, EntryType } from '@/types'

const props=defineProps<{entry:Entry}>(), emit=defineEmits<{saved:[]}>()
const labels:Record<EntryType,string>={TODO:'待办',DIARY:'日记',NOTE:'备忘',IDEA:'想法'}
const type=ref<EntryType>(props.entry.type),title=ref(props.entry.title||''),content=ref(props.entry.content||'')
const toLocal=(v?:string)=>v?new Date(new Date(v).getTime()-new Date(v).getTimezoneOffset()*60000).toISOString().slice(0,16):''
const occurredAt=ref(toLocal(props.entry.occurredAt)),dueAt=ref(toLocal(props.entry.dueAt)),remindAt=ref('')
const saving=ref(false),completing=ref(false),error=ref('')
async function save(){saving.value=true;error.value='';try{await unwrap(api.patch(`/entries/${props.entry.id}`,{type:type.value,title:title.value,content:content.value,occurredAt:occurredAt.value?new Date(occurredAt.value).toISOString():undefined,dueAt:dueAt.value?new Date(dueAt.value).toISOString():undefined}));if(remindAt.value)await unwrap(api.post('/reminders',{entryId:props.entry.id,remindAt:new Date(remindAt.value).toISOString()}));emit('saved')}catch(e:any){error.value=e.userMessage}finally{saving.value=false}}
async function complete(){completing.value=true;error.value='';try{await unwrap(api.post(`/entries/${props.entry.id}/complete`));emit('saved')}catch(e:any){error.value=e.userMessage}finally{completing.value=false}}
</script>

<template>
  <form @submit.prevent="save">
    <div class="type-picker"><button v-for="item in ['TODO','DIARY','NOTE','IDEA'] as EntryType[]" :key="item" type="button" :class="['badge',`badge-${item}`,{active:type===item}]" @click="type=item">{{ labels[item] }}</button></div>
    <section v-if="entry.type==='TODO'" class="task-status" :class="{done:entry.status==='DONE'}"><span class="status-icon"><CircleCheckBig :size="21"/></span><div><strong>{{entry.status==='DONE'?'这项待办已完成':'这项待办等待你确认'}}</strong><p>{{entry.status==='DONE'?'完成状态已经保存。':'收到提醒只代表消息已送达，事情做完后在这里确认。'}}</p></div><button v-if="entry.status!=='DONE'" type="button" class="btn complete-btn" :disabled="completing" @click="complete"><span v-if="completing" class="spinner"/><CircleCheckBig v-else :size="16"/>标记为完成</button></section>
    <label class="field"><span class="label">标题</span><input v-model="title" class="input" placeholder="简短标题"/></label>
    <label class="field"><span class="label">内容</span><textarea v-model="content" class="textarea" rows="6" placeholder="详细内容"/></label>
    <div class="form-grid"><label class="field"><span class="label">发生时间</span><input v-model="occurredAt" class="input" type="datetime-local"/></label><label v-if="type==='TODO'" class="field"><span class="label">截止时间</span><input v-model="dueAt" class="input" type="datetime-local"/></label></div>
    <label class="field"><span class="label"><Bell :size="13"/> 新增提醒</span><input v-model="remindAt" class="input" type="datetime-local"/></label>
    <p v-if="error" class="error">{{error}}</p>
    <div class="actions"><button class="btn btn-primary" :disabled="saving"><span v-if="saving" class="spinner"/><Save v-else :size="16"/>保存</button></div>
  </form>
</template>

<style scoped>.type-picker{display:flex;gap:7px;margin-bottom:20px}.type-picker .badge{border:1px solid transparent;opacity:.5}.type-picker .active{opacity:1;border-color:currentColor}.task-status{display:grid;grid-template-columns:42px minmax(0,1fr) auto;align-items:center;gap:13px;margin:0 0 22px;padding:14px;border:1px solid color-mix(in oklch,var(--todo-foreground) 18%,var(--border));border-radius:calc(var(--radius)*.7);background:color-mix(in oklch,var(--todo) 45%,var(--card))}.task-status.done{border-color:color-mix(in oklch,var(--success) 28%,var(--border));background:color-mix(in oklch,var(--success) 12%,var(--card))}.status-icon{width:42px;height:42px;display:grid;place-items:center;border-radius:calc(var(--radius)*.55);background:var(--card);color:var(--todo-foreground)}.task-status.done .status-icon{color:var(--success-foreground)}.task-status strong{font-size:12px}.task-status p{margin:4px 0 0;color:var(--muted-foreground);font-size:10px;line-height:1.5}.complete-btn{background:var(--success);color:var(--success-foreground);white-space:nowrap}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.label{display:flex;align-items:center;gap:5px}.actions{display:flex;justify-content:flex-end}@media(max-width:650px){.task-status{grid-template-columns:38px 1fr}.complete-btn{grid-column:1/-1;width:100%}}@media(max-width:520px){.form-grid{grid-template-columns:1fr}}</style>
