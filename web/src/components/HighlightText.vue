<script setup lang="ts">
import { computed } from 'vue'
const props=defineProps<{text?:string;query:string}>()
const parts=computed(()=>{const text=props.text||'',q=props.query.trim();if(!q)return[{text,hit:false}];const out:{text:string;hit:boolean}[]=[];let from=0,index=text.toLocaleLowerCase().indexOf(q.toLocaleLowerCase());while(index>=0){if(index>from)out.push({text:text.slice(from,index),hit:false});out.push({text:text.slice(index,index+q.length),hit:true});from=index+q.length;index=text.toLocaleLowerCase().indexOf(q.toLocaleLowerCase(),from)}if(from<text.length)out.push({text:text.slice(from),hit:false});return out})
</script>
<template><span><template v-for="(part,i) in parts" :key="i"><mark v-if="part.hit">{{part.text}}</mark><template v-else>{{part.text}}</template></template></span></template>
<style scoped>mark{background:var(--idea);color:var(--idea-foreground);border-radius:3px;padding:0 2px}</style>

