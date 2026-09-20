<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, KeyRound, ShieldCheck } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth=useAuthStore(),router=useRouter(),username=ref(''),password=ref(''),confirm=ref(''),busy=ref(false),error=ref('')
const setup=computed(()=>!auth.initialized)
onMounted(async()=>{try{await auth.check()}catch{}})
async function submit(){if(setup.value&&password.value!==confirm.value){error.value='两次输入的密码不一致';return}busy.value=true;error.value='';try{setup.value?await auth.setup(username.value,password.value):await auth.login(username.value,password.value);router.replace('/')}catch(e:any){error.value=e.userMessage||'操作失败'}finally{busy.value=false}}
</script>

<template>
  <main class="login app-bg">
    <section class="intro">
      <div class="orb"><img src="/logo.png" alt="RecallHub"/></div>
      <p class="eyebrow">PRIVATE MEMORY SYSTEM</p>
      <h1>把散落的日子，<br/><em>一页页收回来。</em></h1>
      <p class="intro-copy">待办、日记、念头与提醒，都安静地留在自己的电脑里。</p>
      <div class="privacy"><ShieldCheck :size="18"/><span>单用户 · 本地数据 · 私人空间</span></div>
    </section>
    <section class="login-panel card">
      <div><p class="eyebrow">{{setup?'FIRST STEP':'WELCOME BACK'}}</p><h2>{{setup?'创建你的私人空间':'回到 RecallHub'}}</h2><p class="muted">{{setup?'只需设置一次登录账户。':'继续整理今天的记忆。'}}</p></div>
      <form @submit.prevent="submit">
        <label class="field"><span class="label">用户名</span><input v-model="username" class="input" autocomplete="username" required autofocus placeholder="你的名字"/></label>
        <label class="field"><span class="label">密码</span><div class="input-icon"><KeyRound :size="16"/><input v-model="password" type="password" autocomplete="current-password" required minlength="8" placeholder="至少 8 位"/></div></label>
        <label v-if="setup" class="field"><span class="label">确认密码</span><input v-model="confirm" class="input" type="password" required minlength="8" placeholder="再输入一次"/></label>
        <p v-if="error" class="error">{{error}}</p>
        <button class="btn btn-primary submit" :disabled="busy"><span v-if="busy" class="spinner"/><template v-else>{{setup?'开始使用':'进入 RecallHub'}}<ArrowRight :size="17"/></template></button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login{min-height:100vh;display:grid;grid-template-columns:1.2fr .8fr;align-items:center;gap:8vw;padding:7vw}.intro{max-width:650px}.orb{width:88px;height:88px;display:grid;place-items:center;margin-bottom:28px;filter:drop-shadow(0 18px 28px color-mix(in oklch,var(--primary) 24%,transparent));transform:rotate(-3deg)}.orb img{display:block;width:100%;height:100%;object-fit:contain}h1{font-family:"Noto Serif SC","Songti SC",serif;font-size:clamp(44px,6vw,84px);line-height:1.08;letter-spacing:-.06em;margin:15px 0 25px;font-weight:600}h1 em{color:var(--primary);font-style:normal}.intro-copy{max-width:460px;color:var(--muted-foreground);font-size:16px;line-height:1.8}.privacy{display:flex;align-items:center;gap:8px;margin-top:38px;font-size:12px;color:var(--muted-foreground)}.login-panel{max-width:480px;width:100%;justify-self:end;padding:38px}.login-panel h2{font:600 28px/1.2 "Noto Serif SC","Songti SC",serif;margin:8px 0}.login-panel>div>.muted{font-size:13px;margin:0 0 30px}.input-icon{display:flex;align-items:center;gap:10px;background:var(--background);border:1px solid var(--input);border-radius:calc(var(--radius)*.7);padding:0 13px}.input-icon input{width:100%;padding:11px 0;border:0;outline:0;background:transparent}.submit{width:100%;height:48px;margin-top:8px}@media(max-width:800px){.login{display:block;padding:44px 20px}.intro{margin:0 auto 40px;text-align:center}.orb{margin:0 auto 25px}h1{font-size:42px}.intro-copy,.privacy{margin-left:auto;margin-right:auto;justify-content:center}.login-panel{margin:auto;padding:28px 22px}}
</style>
