<script setup lang="ts">
import { ref } from 'vue'
import { Search, SlidersHorizontal } from 'lucide-vue-next'
import { api, unwrap } from '@/api/client'
import type { Entry, EntryType } from '@/types'
import HighlightText from '@/components/HighlightText.vue'

const q=ref(''),type=ref<EntryType|''>(''),items=ref<Entry[]>([]),total=ref(0),searched=ref(false),loading=ref(false)
async function search(){if(!q.value.trim())return;loading.value=true;try{const data=await unwrap<{items:Entry[];total:number}>(api.get('/search',{params:{q:q.value,type:type.value||undefined}}));items.value=data.items;total.value=data.total;searched.value=true}finally{loading.value=false}}
const labels={TODO:'待办',DIARY:'日记',NOTE:'备忘',IDEA:'想法'}
</script>

<template>
  <div class="page search-page">
    <header class="page-header"><div><p class="eyebrow">FIND IT AGAIN</p><h1 class="page-title">搜索</h1><p class="page-subtitle">一句话、一个名字，找回当时留下的痕迹。</p></div></header>
    <form class="search-box card" @submit.prevent="search"><Search :size="23"/><input v-model="q" placeholder="搜索你的记录……" autofocus/><button class="btn btn-primary" :disabled="loading">搜索</button></form>
    <div class="search-filter"><SlidersHorizontal :size="14"/><button v-for="f in [{v:'',l:'全部'},{v:'DIARY',l:'日记'},{v:'TODO',l:'待办'},{v:'IDEA',l:'想法'},{v:'NOTE',l:'备忘'}]" :key="f.v" :class="{active:type===f.v}" @click="type=f.v as any;searched&&search()">{{f.l}}</button></div>
    <div v-if="searched" class="result-meta">找到 {{total}} 条关于「{{q}}」的记录</div>
    <div v-if="searched&&!items.length" class="empty">没有找到相关记录，换个词试试。</div>
    <div class="results"><article v-for="e in items" :key="e.id" class="result card"><div class="result-top"><span class="badge" :class="`badge-${e.type}`">{{labels[e.type]}}</span><time>{{new Date(e.occurredAt||e.createdAt).toLocaleDateString('zh-CN')}}</time></div><h2 v-if="e.title"><HighlightText :text="e.title" :query="q"/></h2><p v-if="e.content"><HighlightText :text="e.content" :query="q"/></p></article></div>
  </div>
</template>

<style scoped>
.search-page{max-width:1160px}.search-box{position:relative;display:flex;align-items:center;gap:16px;min-height:76px;padding:11px 11px 11px 24px;overflow:hidden;border-color:color-mix(in oklch,var(--primary) 20%,var(--border));box-shadow:0 24px 70px color-mix(in oklch,var(--primary) 9%,transparent)}.search-box::after{content:"";position:absolute;width:120px;height:120px;right:75px;top:-80px;border:22px solid color-mix(in oklch,var(--primary) 7%,transparent);border-radius:50%;pointer-events:none}.search-box>svg{position:relative;z-index:1;color:var(--primary)}.search-box input{position:relative;z-index:1;min-width:0;flex:1;border:0;outline:0;background:transparent;font:550 20px/1.4 var(--font-display);letter-spacing:-.02em}.search-box input::placeholder{color:var(--muted-foreground)}.search-box .btn{position:relative;z-index:1;min-height:52px;padding:0 23px}.search-filter{display:flex;align-items:center;gap:4px;width:max-content;max-width:100%;margin:18px 0 42px;padding:5px;border:1px solid var(--border);border-radius:999px;background:color-mix(in oklch,var(--card) 75%,transparent);color:var(--muted-foreground)}.search-filter>svg{margin-left:7px}.search-filter button{border:0;background:transparent;color:inherit;padding:8px 12px;border-radius:999px;font-size:9px;font-weight:900;letter-spacing:.04em}.search-filter button:hover{background:var(--muted);color:var(--foreground)}.search-filter button.active{background:var(--secondary);color:var(--secondary-foreground);box-shadow:0 6px 18px color-mix(in oklch,var(--foreground) 6%,transparent)}.result-meta{display:flex;align-items:center;gap:10px;font-size:10px;font-weight:800;letter-spacing:.04em;color:var(--muted-foreground);margin-bottom:14px}.result-meta::before{content:"";width:28px;height:1px;background:var(--primary)}.results{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:13px}.result{position:relative;min-height:160px;padding:22px;overflow:hidden;transition:transform .2s,border .2s,box-shadow .2s}.result::after{content:"";position:absolute;right:-26px;bottom:-34px;width:80px;height:80px;border:13px solid color-mix(in oklch,var(--primary) 5%,transparent);border-radius:50%}.result:hover{transform:translateY(-4px);border-color:color-mix(in oklch,var(--primary) 28%,var(--border));box-shadow:0 18px 46px color-mix(in oklch,var(--foreground) 8%,transparent)}.result-top{display:flex;justify-content:space-between;align-items:center}.result time{font-size:9px;font-weight:800;color:var(--muted-foreground)}.result h2{font:650 18px/1.45 var(--font-display);letter-spacing:-.025em;margin:14px 0 0}.result p{font-size:12px;color:var(--muted-foreground);line-height:1.75;white-space:pre-wrap;margin:8px 0 0}@media(max-width:680px){.results{grid-template-columns:1fr}.search-filter{overflow:auto}.search-box{min-height:66px;padding-left:17px}.search-box input{font-size:16px}.search-box .btn{min-height:46px;padding:0 14px}}
.search-filter button{padding:9px 15px;font-size:12px;letter-spacing:.02em}
</style>
