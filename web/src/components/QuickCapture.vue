<script setup lang="ts">
import { ref, computed } from 'vue'
import { Bell, Send, Sparkles } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import type { EntryType } from '@/types'

const emit = defineEmits<{ saved: [] }>()
const type = ref<EntryType>('NOTE'), title = ref(''), content = ref(''), dueAt = ref(''), remindAt = ref('')
const expanded = ref(false), saving = ref(false), error = ref('')
const types: { value: EntryType; label: string }[] = [{value:'NOTE',label:'备忘'},{value:'TODO',label:'待办'},{value:'DIARY',label:'日记'},{value:'IDEA',label:'想法'}]
const placeholder = computed(() => type.value==='TODO'?'要完成什么？':type.value==='DIARY'?'今天发生了什么？':type.value==='IDEA'?'刚刚闪过什么念头？':'记点什么……')
const localIso = (value: string) => value ? new Date(value).toISOString() : undefined

async function save() {
  const text = title.value.trim() || content.value.trim(); if (!text) return
  saving.value=true; error.value=''
  try {
    await unwrap(api.post('/captures', {
      entry: { type:type.value, title:type.value==='TODO'?text:(title.value.trim()||undefined), content:type.value==='TODO'?(content.value.trim()||undefined):(content.value.trim()||text), dueAt:localIso(dueAt.value) },
      reminder: remindAt.value ? { remindAt:localIso(remindAt.value) } : undefined,
      source: { sourceType:'WEB', channel:'WEB' },
    }))
    title.value='';content.value='';dueAt.value='';remindAt.value='';expanded.value=false;emit('saved')
  } catch(e:any){error.value=e.userMessage} finally{saving.value=false}
}
</script>

<template>
  <form class="capture card" @submit.prevent="save">
    <div class="capture-label"><span>QUICK CAPTURE</span><small>TEXT → MEMORY</small></div>
    <div class="capture-main"><Sparkles :size="19" class="spark"/><input v-model="title" :placeholder="placeholder" aria-label="快速记录" @focus="expanded=true"/><button class="send" :disabled="saving || !title.trim()" aria-label="保存"><span v-if="saving" class="spinner"/><Send v-else :size="18"/></button></div>
    <div v-if="expanded" class="capture-more">
      <div class="type-row"><button v-for="item in types" :key="item.value" type="button" :class="['type-pill',`badge-${item.value}`,{active:type===item.value}]" @click="type=item.value">{{ item.label }}</button></div>
      <textarea v-model="content" class="textarea" placeholder="补充更多内容（可选）" />
      <div class="time-grid">
        <label v-if="type==='TODO'"><span>截止时间</span><input v-model="dueAt" class="input" type="datetime-local"/></label>
        <label><span><Bell :size="12"/> 提醒时间</span><input v-model="remindAt" class="input" type="datetime-local"/></label>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </form>
</template>

<style scoped>
.capture{position:relative;overflow:hidden;border-color:color-mix(in oklch,var(--primary) 18%,var(--border));box-shadow:0 22px 60px color-mix(in oklch,var(--primary) 8%,transparent)}.capture::after{content:"";position:absolute;right:-48px;top:-70px;width:150px;height:150px;border:24px solid color-mix(in oklch,var(--primary) 7%,transparent);border-radius:50%;pointer-events:none}.capture-label{position:relative;z-index:1;display:flex;justify-content:space-between;align-items:center;padding:13px 18px 4px;color:var(--primary);font-size:8px;font-weight:950;letter-spacing:.18em}.capture-label small{color:var(--muted-foreground);font-size:7px;letter-spacing:.14em}.capture-main{position:relative;z-index:1;display:flex;align-items:center;gap:14px;padding:4px 9px 11px 18px}.spark{color:var(--primary)}.capture-main input{flex:1;min-width:0;padding:14px 0;border:0;outline:0;background:transparent;font-family:var(--font-display);font-size:17px}.capture-main input::placeholder{color:var(--muted-foreground)}.send{width:48px;height:48px;border:1px solid color-mix(in oklch,var(--primary-foreground) 15%,transparent);border-radius:calc(var(--radius)*.68);background:var(--primary);color:var(--primary-foreground);display:grid;place-items:center;box-shadow:0 10px 26px color-mix(in oklch,var(--primary) 25%,transparent);transition:.18s}.send:not(:disabled):hover{transform:translateY(-2px) rotate(-3deg)}.send:disabled{opacity:.3;box-shadow:none}.capture-more{position:relative;z-index:1;padding:4px 18px 18px;border-top:1px solid var(--border);background:color-mix(in oklch,var(--muted) 24%,transparent)}.type-row{display:flex;flex-wrap:wrap;gap:7px;padding:14px 0}.type-pill{border:1px solid transparent;border-radius:999px;padding:6px 11px;font-size:10px;font-weight:900;opacity:.52;transition:.18s}.type-pill:hover{opacity:.8;transform:translateY(-1px)}.type-pill.active{opacity:1;border-color:currentColor}.time-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:12px}.time-grid span{display:flex;align-items:center;gap:5px;margin-bottom:6px;font-size:9px;font-weight:900;letter-spacing:.06em;color:var(--muted-foreground)}@media(max-width:560px){.time-grid{grid-template-columns:1fr}.capture-main input{font-size:15px}.capture-label small{display:none}}
.type-pill{padding:7px 12px;font-size:12px}
</style>
