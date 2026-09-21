<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Bot, Check, Cloud, Database, Download, KeyRound, Moon, Palette, Plus, Save, ShieldCheck, Sparkles, Sun, Trash2, Upload, Wifi, WifiOff } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import { useThemeStore } from '@/stores/theme'
import { parseThemeCss } from '@/theme/parser'
import type { Theme } from '@/types'
import AppSelect, { type SelectOption } from '@/components/AppSelect.vue'

const themeStore=useThemeStore(),route=useRoute(),section=ref<'appearance'|'ai'|'openclaw'|'data'|'security'>('appearance')
const themes=ref<Theme[]>([]),selectedTheme=ref<number>(),css=ref(''),themeName=ref('我的主题'),themeError=ref(''),preview=ref<Theme|null>(null)
const openclaw=ref<{online:boolean;baseUrl:string;tokenConfigured:boolean}|null>(null),targets=ref<any[]>([]),tokens=ref<any[]>([]),newToken=ref('')
const openclawUrl=ref('')
const hookToken=ref('')
const aiBaseUrl=ref(''),aiApiKey=ref(''),aiModel=ref(''),aiKeyConfigured=ref(false),aiSaving=ref(false),aiError=ref('')
const targetForm=ref({name:'我的微信',channel:'openclaw-weixin',target:'',accountId:'',agentId:'main',enabled:true,isDefault:true})
const settings=ref<Record<string,string>>({}),notice=ref('')
const tabs=[{v:'appearance',l:'外观与主题',i:Palette},{v:'ai',l:'AI 设置',i:Bot},{v:'openclaw',l:'OpenClaw',i:Cloud},{v:'security',l:'访问令牌',i:KeyRound},{v:'data',l:'备份与导出',i:Database}] as const
const themeOptions=ref<SelectOption[]>([])
const retentionOptions:SelectOption[]=[{value:'NONE',label:'不保存'},{value:'ON_DEMAND',label:'按需保存'},{value:'ALL',label:'全部保存'}]

async function load(){
  const jobs=[unwrap<Theme[]>(api.get('/themes')).then(v=>{themes.value=v;themeOptions.value=v.map(t=>({value:t.id,label:`${t.name}${t.builtin?' · 内置':''}`}));selectedTheme.value=themeStore.current?.id||v[0]?.id}),unwrap<Record<string,string>>(api.get('/settings')).then(v=>{settings.value=v;openclawUrl.value=v.openclaw_base_url||'';aiBaseUrl.value=v.ai_base_url||'https://api.openai.com/v1';aiModel.value=v.ai_model||'';aiKeyConfigured.value=v.ai_api_key_configured==='true'}),unwrap<any[]>(api.get('/notification-targets')).then(v=>targets.value=v),unwrap<any[]>(api.get('/tokens')).then(v=>tokens.value=v),unwrap<any>(api.get('/system/openclaw/status')).then(v=>openclaw.value=v).catch(()=>{})]
  await Promise.all(jobs)
}
function parse(){themeError.value='';try{const parsed=parseThemeCss(css.value);preview.value={id:0,name:themeName.value,slug:'preview',builtin:false,...parsed};themeStore.preview(preview.value)}catch(e:any){themeError.value=e.message}}
async function saveTheme(){parse();if(!preview.value)return;const saved=await unwrap<Theme>(api.post('/themes',{name:themeName.value,lightVariables:preview.value.lightVariables,darkVariables:preview.value.darkVariables,sourceCss:preview.value.sourceCss}));await themeStore.activate(saved.id);notice.value='主题已保存并应用';load()}
async function applyTheme(){if(selectedTheme.value){await themeStore.activate(selectedTheme.value);notice.value='主题已应用'}}
async function addTarget(){await unwrap(api.post('/notification-targets',targetForm.value));targetForm.value.target='';targetForm.value.accountId='';targets.value=await unwrap(api.get('/notification-targets'));notice.value='通知渠道已保存'}
async function removeTarget(id:number){await api.delete(`/notification-targets/${id}`);targets.value=targets.value.filter(x=>x.id!==id)}
async function createToken(){const value=await unwrap<any>(api.post('/tokens',{name:'OpenClaw',scopes:['ENTRY_READ','ENTRY_WRITE','REMINDER_READ','REMINDER_WRITE','SEARCH','JOURNAL_READ','JOURNAL_WRITE']}));newToken.value=value.token;tokens.value=await unwrap(api.get('/tokens'))}
async function revokeToken(id:number){await api.delete(`/tokens/${id}`);tokens.value=tokens.value.filter(x=>x.id!==id)}
function copyToken(){window.navigator.clipboard.writeText(newToken.value);notice.value='Token 已复制'}
function dismissNotice(){window.setTimeout(()=>notice.value='',1800)}
async function updateSettings(key:string,value:string){settings.value[key]=value;await unwrap(api.put('/settings',{[key]:value}));notice.value='设置已保存'}
async function saveOpenClawConfig(){const values:Record<string,string>={openclaw_base_url:openclawUrl.value.trim()};if(hookToken.value.trim())values.openclaw_hook_token=hookToken.value.trim();await unwrap(api.put('/settings',values));hookToken.value='';openclaw.value=await unwrap(api.get('/system/openclaw/status'));notice.value='OpenClaw 配置已保存'}
async function clearHookToken(){await unwrap(api.put('/settings',{openclaw_hook_token:''}));openclaw.value=await unwrap(api.get('/system/openclaw/status'));notice.value='已清除数据库 Token，将使用环境变量'}
async function saveAiConfig(){
  aiError.value=''
  if(!aiBaseUrl.value.trim()||!aiModel.value.trim()){aiError.value='请填写 Base URL 和模型名称';return}
  if(!aiKeyConfigured.value&&!aiApiKey.value.trim()){aiError.value='请填写 API Key';return}
  aiSaving.value=true
  try{
    const values:Record<string,string>={ai_base_url:aiBaseUrl.value.trim(),ai_model:aiModel.value.trim()}
    if(aiApiKey.value.trim())values.ai_api_key=aiApiKey.value.trim()
    const saved=await unwrap<Record<string,string>>(api.put('/settings',values))
    aiApiKey.value='';aiKeyConfigured.value=saved.ai_api_key_configured==='true';notice.value='AI 设置已保存'
  }catch(error:any){aiError.value=error.userMessage||'AI 设置保存失败'}finally{aiSaving.value=false}
}
async function clearAiApiKey(){
  const saved=await unwrap<Record<string,string>>(api.put('/settings',{ai_api_key:''}))
  aiApiKey.value='';aiKeyConfigured.value=saved.ai_api_key_configured==='true';notice.value='API Key 已清除'
}
async function backup(){const value=await unwrap<any>(api.post('/backups'));notice.value=`备份已创建：${value.file}`}
function pickFile(ev:Event){const file=(ev.target as HTMLInputElement).files?.[0];if(!file)return;file.text().then(text=>{css.value=text;themeName.value=file.name.replace(/\.css$/i,'');parse()})}
onMounted(()=>{if(route.query.section==='ai')section.value='ai';load()})
</script>

<template>
  <div class="page settings-page">
    <header class="page-header"><div><p class="eyebrow">MAKE IT YOURS</p><h1 class="page-title">设置</h1><p class="page-subtitle">调整 RecallHub 的外观、连接与数据保存方式。</p></div></header>
    <div class="settings-layout">
      <nav class="settings-nav"><button v-for="tab in tabs" :key="tab.v" :class="{active:section===tab.v}" @click="section=tab.v"><component :is="tab.i" :size="16"/>{{tab.l}}</button></nav>
      <main>
        <template v-if="section==='appearance'">
          <section class="setting-block card"><div class="block-head"><div><h2>外观模式</h2><p>可以跟随设备，也可以固定明暗模式。</p></div></div><div class="mode-grid"><button v-for="mode in [{v:'SYSTEM',l:'跟随系统',i:Palette},{v:'LIGHT',l:'浅色',i:Sun},{v:'DARK',l:'深色',i:Moon}]" :key="mode.v" :class="{active:themeStore.mode===mode.v}" @click="themeStore.setMode(mode.v as any);updateSettings('appearance',mode.v)"><component :is="mode.i" :size="19"/>{{mode.l}}<Check v-if="themeStore.mode===mode.v" :size="15"/></button></div></section>
          <section class="setting-block card"><div class="block-head"><div><h2>已保存主题</h2><p>切换后会立即应用到所有页面。</p></div></div><div class="theme-row"><AppSelect v-model="selectedTheme" :options="themeOptions"/><button class="btn btn-primary" @click="applyTheme">应用</button><a v-if="selectedTheme" class="btn btn-secondary" :href="`/api/v1/themes/${selectedTheme}/export`"><Download :size="15"/>导出</a></div></section>
          <section class="setting-block card"><div class="block-head"><div><h2>导入 globals.css</h2><p>只读取 <code>:root</code> 与 <code>.dark</code> 中受支持的变量。</p></div><label class="btn btn-secondary file"><Upload :size="15"/>选择文件<input type="file" accept=".css,text/css" @change="pickFile"/></label></div><label class="field"><span class="label">主题名称</span><input v-model="themeName" class="input"/></label><textarea v-model="css" class="textarea css-editor" spellcheck="false" placeholder=":root {
  --background: oklch(1 0 0);
  --primary: oklch(0.56 0.16 250);
}

.dark {
  --background: oklch(0.17 0.025 255);
}" @input="preview=null"/><p v-if="themeError" class="error">{{themeError}}</p><div class="preview-card" :class="{ready:preview}"><span class="badge badge-TODO">待办</span><h3>{{preview?'预览已经准备好':'粘贴主题后开始预览'}}</h3><p>Card、文字、按钮和语义色都会使用新的变量。</p><button class="btn btn-primary">Primary Button</button></div><div class="block-actions"><button class="btn btn-secondary" @click="parse">实时预览</button><button class="btn btn-primary" :disabled="!css" @click="saveTheme"><Save :size="15"/>保存并应用</button></div></section>
        </template>
        <template v-else-if="section==='ai'">
          <section class="setting-block card ai-settings-card">
            <div class="ai-heading">
              <span class="ai-mark"><Sparkles :size="20" /></span>
              <div>
                <p class="eyebrow">RESPONSES API</p>
                <h2>日记整理模型</h2>
                <p>连接兼容 OpenAI Responses 接口的服务，用当天的原始片段生成日记。</p>
              </div>
              <span :class="['credential-state',{configured:aiKeyConfigured}]">
                <ShieldCheck :size="14" />{{aiKeyConfigured?'密钥已配置':'等待配置'}}
              </span>
            </div>
            <div class="ai-form">
              <label class="field ai-url-field"><span class="label">Base URL</span><input v-model="aiBaseUrl" class="input" type="url" placeholder="https://api.openai.com/v1"/><small>系统会自动在地址末尾调用 <code>/responses</code>；也可以直接填写完整接口地址。</small></label>
              <label class="field"><span class="label">Model</span><input v-model="aiModel" class="input" placeholder="gpt-5-mini" autocomplete="off"/></label>
              <label class="field ai-key-field"><span class="label">API Key</span><input v-model="aiApiKey" class="input" type="password" autocomplete="new-password" :placeholder="aiKeyConfigured?'已配置，留空保持不变':'粘贴 API Key'"/><small>密钥只发送到 RecallHub 服务端，保存后不会再次回显。</small></label>
            </div>
            <p v-if="aiError" class="error ai-form-error">{{aiError}}</p>
            <div class="ai-actions">
              <button v-if="aiKeyConfigured" class="clear-token" type="button" @click="clearAiApiKey">清除 API Key</button>
              <button class="btn btn-primary" :disabled="aiSaving" @click="saveAiConfig"><Save :size="15"/>{{aiSaving?'保存中…':'保存 AI 设置'}}</button>
            </div>
          </section>
          <section class="ai-note">
            <Bot :size="17" />
            <p><strong>怎么使用？</strong> 保存后回到日记页，选择有原始片段的日期，点击“AI 一键整理”。再次整理会更新整理后的正文，原始片段始终保留。</p>
          </section>
        </template>
        <template v-else-if="section==='openclaw'">
          <section class="setting-block card"><div class="status-row"><span :class="['status-dot',{online:openclaw?.online}]"/><div><h2>OpenClaw Gateway</h2><p>{{openclaw?.baseUrl||'尚未检测'}}</p></div><span class="status-label"><Wifi v-if="openclaw?.online" :size="15"/><WifiOff v-else :size="15"/>{{openclaw?.online?'在线':'未连接'}}</span></div><div class="gateway-form"><label class="field"><span class="label">Base URL</span><input v-model="openclawUrl" class="input" type="url" placeholder="http://127.0.0.1:18789"/></label><button class="btn btn-primary save-gateway" @click="saveOpenClawConfig"><Save :size="15"/>保存并检测</button><label class="field token-field"><span class="label">Hook Token</span><input v-model="hookToken" class="input" type="password" autocomplete="new-password" :placeholder="openclaw?.tokenConfigured?'已配置，留空保持不变':'粘贴 OpenClaw Hook Token'"/></label><div class="token-status"><span :class="['token-state',{configured:openclaw?.tokenConfigured}]">{{openclaw?.tokenConfigured?'Token 已配置':'Token 未配置'}}</span><button v-if="openclaw?.tokenConfigured" class="clear-token" type="button" @click="clearHookToken">清除数据库 Token</button></div><small>数据库配置优先；留空时使用 OPENCLAW_BASE_URL 和 OPENCLAW_HOOK_TOKEN 环境变量。Token 保存后不会再次回显。</small></div></section>
          <section class="setting-block card"><div class="block-head"><div><h2>提醒渠道</h2><p>Target 和 Account ID 只保存在本地数据库。</p></div></div><div v-for="t in targets" :key="t.id" class="target"><div><strong>{{t.name}}</strong><p>{{t.channel}} · {{t.target}}</p></div><span v-if="t.isDefault" class="badge badge-TODO">默认</span><button class="icon-btn" @click="removeTarget(t.id)"><Trash2 :size="15"/></button></div><div class="form-grid"><label class="field"><span class="label">名称</span><input v-model="targetForm.name" class="input"/></label><label class="field"><span class="label">Channel</span><input v-model="targetForm.channel" class="input"/></label><label class="field"><span class="label">微信 Target</span><input v-model="targetForm.target" class="input" placeholder="...@im.wechat"/></label><label class="field"><span class="label">Account ID</span><input v-model="targetForm.accountId" class="input"/></label></div><button class="btn btn-primary" :disabled="!targetForm.target" @click="addTarget"><Plus :size="15"/>添加渠道</button></section>
        </template>
        <template v-else-if="section==='security'">
          <section class="setting-block card"><div class="block-head"><div><h2>OpenClaw API Token</h2><p>明文只在创建后显示一次，请立即复制到 Skill 配置中。</p></div><button class="btn btn-primary" @click="createToken"><Plus :size="15"/>生成 Token</button></div><div v-if="newToken" class="token-once"><code>{{newToken}}</code><button class="btn btn-secondary" @click="copyToken">复制</button></div><div v-for="token in tokens" :key="token.id" class="target"><div><strong>{{token.name}}</strong><p><code>{{token.prefix}}••••••••</code> · {{token.enabled?'有效':'已停用'}}</p></div><button class="icon-btn" @click="revokeToken(token.id)"><Trash2 :size="15"/></button></div></section>
        </template>
        <template v-else>
          <section class="setting-block card"><div class="block-head"><div><h2>原始聊天保存</h2><p>控制 OpenClaw 原文进入 RecallHub 的方式。</p></div></div><AppSelect :model-value="settings.raw_chat_retention" :options="retentionOptions" @update:model-value="updateSettings('raw_chat_retention',String($event))"/></section>
          <section class="setting-block card"><div class="block-head"><div><h2>数据导出</h2><p>导出结构化 JSON，或者适合阅读的 Markdown 压缩包。</p></div></div><div class="export-row"><a class="btn btn-secondary" href="/api/v1/export/json"><Download :size="15"/>导出 JSON</a><a class="btn btn-secondary" href="/api/v1/export/markdown"><Download :size="15"/>导出 Markdown</a></div></section>
          <section class="setting-block card"><div class="block-head"><div><h2>数据库备份</h2><p>调用本机 mysqldump，备份文件保存在配置目录。</p></div><button class="btn btn-primary" @click="backup"><Database :size="15"/>立即备份</button></div></section>
        </template>
      </main>
    </div>
    <Transition name="toast"><div v-if="notice" class="toast" @animationend="dismissNotice">{{notice}}</div></Transition>
  </div>
</template>

<style scoped>
.settings-layout{display:grid;grid-template-columns:180px 1fr;gap:28px;align-items:start}.settings-nav{position:sticky;top:30px;display:grid;gap:5px}.settings-nav button{display:flex;align-items:center;gap:9px;border:0;background:transparent;padding:11px 12px;border-radius:calc(var(--radius)*.65);color:var(--muted-foreground);font-size:12px;font-weight:800;text-align:left}.settings-nav button.active{background:var(--secondary);color:var(--secondary-foreground)}.setting-block{padding:24px;margin-bottom:15px}.block-head{display:flex;align-items:flex-start;justify-content:space-between;gap:15px;margin-bottom:22px}.block-head h2,.status-row h2{font-size:15px;margin:0 0 5px}.block-head p,.status-row p{margin:0;color:var(--muted-foreground);font-size:11px}.mode-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:9px}.mode-grid button{position:relative;display:flex;align-items:center;justify-content:center;gap:7px;height:66px;border:1px solid var(--border);border-radius:calc(var(--radius)*.7);background:var(--background);font-size:11px;font-weight:800}.mode-grid button.active{border-color:var(--primary);box-shadow:0 0 0 2px color-mix(in oklch,var(--primary) 12%,transparent)}.theme-row,.export-row{display:flex;gap:9px}.theme-row :deep(.app-select){flex:1}.file input{display:none}.css-editor{min-height:230px;font:12px/1.65 Consolas,monospace}.preview-card{margin-top:14px;padding:22px;border:1px solid var(--border);border-radius:var(--radius);background:var(--card)}.preview-card h3{font-family:serif;margin:12px 0 6px}.preview-card p{font-size:11px;color:var(--muted-foreground);margin:0 0 14px}.block-actions{display:flex;justify-content:flex-end;gap:9px;margin-top:15px}.status-row{display:flex;align-items:center;gap:13px}.status-dot{width:11px;height:11px;border-radius:99px;background:var(--destructive);box-shadow:0 0 0 5px color-mix(in oklch,var(--destructive) 12%,transparent)}.status-dot.online{background:var(--success);box-shadow:0 0 0 5px color-mix(in oklch,var(--success) 15%,transparent)}.status-row>div{flex:1}.status-label{display:flex;align-items:center;gap:5px;font-size:11px;color:var(--muted-foreground)}.target{display:flex;align-items:center;gap:10px;padding:12px 0;border-bottom:1px solid var(--border)}.target>div{flex:1}.target strong{font-size:12px}.target p{font-size:10px;color:var(--muted-foreground);margin:3px 0 0}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 12px;margin-top:20px}.token-once{display:flex;gap:10px;align-items:center;padding:12px;background:var(--muted);border-radius:calc(var(--radius)*.7);margin-bottom:12px}.token-once code{flex:1;overflow:auto;font-size:10px}.toast-enter-active,.toast-leave-active{transition:.2s}.toast-enter-from,.toast-leave-to{opacity:0;transform:translateY(8px)}@media(max-width:720px){.settings-layout{grid-template-columns:1fr}.settings-nav{position:static;display:flex;overflow:auto}.settings-nav button{white-space:nowrap}.mode-grid{grid-template-columns:1fr}.form-grid{grid-template-columns:1fr}.theme-row{flex-wrap:wrap}.theme-row :deep(.app-select){flex-basis:100%}}
.gateway-form{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:13px 12px;margin-top:20px;padding-top:18px;border-top:1px solid var(--border)}.gateway-form .field{margin:0}.gateway-form .save-gateway{height:46px;white-space:nowrap}.gateway-form small{grid-column:1/-1;display:block;color:var(--muted-foreground);font-size:10px;line-height:1.5}.token-status{align-self:end;min-height:46px;display:flex;align-items:center;justify-content:flex-end;gap:10px}.token-state{display:inline-flex;align-items:center;gap:6px;color:var(--muted-foreground);font-size:10px;font-weight:800;white-space:nowrap}.token-state::before{content:"";width:7px;height:7px;border-radius:99px;background:var(--destructive)}.token-state.configured::before{background:var(--success)}.clear-token{padding:5px 8px;border:0;border-radius:8px;background:transparent;color:var(--muted-foreground);font-size:10px}.clear-token:hover{background:var(--muted);color:var(--foreground)}@media(max-width:650px){.gateway-form{grid-template-columns:1fr;align-items:stretch}.gateway-form small{grid-column:1}.gateway-form .save-gateway{width:100%}.token-status{justify-content:flex-start}}
.settings-page{max-width:1280px}.settings-layout{grid-template-columns:225px minmax(0,1fr);gap:32px}.settings-nav{padding:8px;border:1px solid color-mix(in oklch,var(--border) 84%,transparent);border-radius:var(--radius);background:color-mix(in oklch,var(--card) 68%,transparent);box-shadow:0 14px 42px color-mix(in oklch,var(--foreground) 4%,transparent);backdrop-filter:blur(16px)}.settings-nav button{position:relative;min-height:47px;padding:0 13px;border:1px solid transparent}.settings-nav button::after{content:"";position:absolute;right:12px;width:5px;height:5px;border-radius:50%;background:var(--primary);opacity:0;transform:scale(0);transition:.18s}.settings-nav button.active{background:var(--primary);color:var(--primary-foreground);border-color:color-mix(in oklch,var(--primary-foreground) 12%,transparent);box-shadow:0 9px 24px color-mix(in oklch,var(--primary) 20%,transparent)}.settings-nav button.active::after{opacity:1;transform:scale(1);background:var(--primary-foreground)}.setting-block{position:relative;padding:28px 30px;margin-bottom:18px;overflow:visible}.setting-block::before{content:"";position:absolute;left:0;top:26px;width:3px;height:34px;border-radius:0 3px 3px 0;background:var(--primary);opacity:.7}.block-head{margin-bottom:25px}.block-head h2,.status-row h2{font:650 18px/1.3 var(--font-display);letter-spacing:-.02em}.block-head p,.status-row p{font-size:10px;line-height:1.65}.mode-grid button{height:76px;flex-direction:column;gap:8px}.theme-row :deep(.app-select){min-width:0}.status-row{padding-bottom:4px}.status-dot{width:12px;height:12px}.target{min-height:64px}.preview-card{padding:26px;background:linear-gradient(135deg,var(--card),color-mix(in oklch,var(--primary) 5%,var(--card)))}@media(max-width:900px){.settings-layout{grid-template-columns:190px minmax(0,1fr);gap:20px}.setting-block{padding:24px}}@media(max-width:720px){.settings-layout{grid-template-columns:1fr}.settings-nav{position:static;display:flex;padding:5px;overflow:auto}.settings-nav button{white-space:nowrap;min-height:40px}.setting-block{padding:23px 19px}}
.ai-settings-card{overflow:hidden;padding:0}.ai-settings-card::before{display:none}.ai-heading{position:relative;display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:17px;padding:29px 30px 25px;border-bottom:1px solid var(--border);background:linear-gradient(125deg,color-mix(in oklch,var(--primary) 10%,var(--card)),var(--card) 58%)}.ai-heading::after{content:"AI";position:absolute;right:22px;bottom:-34px;font:800 96px/1 var(--font-display);letter-spacing:-.08em;color:color-mix(in oklch,var(--primary) 5%,transparent);pointer-events:none}.ai-mark{position:relative;z-index:1;width:46px;height:46px;display:grid;place-items:center;border-radius:14px;background:var(--primary);color:var(--primary-foreground);box-shadow:0 12px 28px color-mix(in oklch,var(--primary) 24%,transparent)}.ai-heading>div{position:relative;z-index:1}.ai-heading .eyebrow{margin:0 0 6px}.ai-heading h2{margin:0 0 6px;font:650 20px/1.25 var(--font-display);letter-spacing:-.03em}.ai-heading p:last-child{margin:0;color:var(--muted-foreground);font-size:10px;line-height:1.6}.credential-state{position:relative;z-index:1;display:inline-flex;align-items:center;gap:6px;padding:7px 9px;border:1px solid color-mix(in oklch,var(--destructive) 20%,var(--border));border-radius:99px;background:color-mix(in oklch,var(--destructive) 6%,var(--card));color:var(--muted-foreground);font-size:9px;font-weight:900;white-space:nowrap}.credential-state.configured{border-color:color-mix(in oklch,var(--success) 28%,var(--border));background:color-mix(in oklch,var(--success) 8%,var(--card));color:var(--success)}.ai-form{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(180px,.75fr);gap:4px 15px;padding:28px 30px 6px}.ai-form .field{margin-bottom:18px}.ai-url-field,.ai-key-field{grid-column:auto}.ai-key-field{grid-column:1/-1}.ai-form small{display:block;margin-top:7px;color:var(--muted-foreground);font-size:9px;line-height:1.55}.ai-form code{font-size:9px}.ai-actions{display:flex;align-items:center;justify-content:flex-end;gap:12px;padding:0 30px 28px}.ai-actions .clear-token{margin-right:auto}.ai-form-error{margin:0 30px 18px}.ai-note{display:flex;gap:12px;align-items:flex-start;padding:5px 10px;color:var(--muted-foreground)}.ai-note svg{flex:none;margin-top:2px;color:var(--primary)}.ai-note p{margin:0;font-size:10px;line-height:1.7}.ai-note strong{color:var(--foreground)}@media(max-width:720px){.ai-heading{grid-template-columns:auto 1fr;padding:24px 20px}.credential-state{grid-column:2;justify-self:start}.ai-form{grid-template-columns:1fr;padding:24px 20px 5px}.ai-key-field{grid-column:auto}.ai-actions{padding:0 20px 24px}.ai-form-error{margin-left:20px;margin-right:20px}}
</style>
