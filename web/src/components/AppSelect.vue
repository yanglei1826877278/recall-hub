<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Check, ChevronDown } from 'lucide-vue-next'

export interface SelectOption { value: string | number; label: string; disabled?: boolean }
const props = defineProps<{ modelValue?: string | number; options: SelectOption[]; placeholder?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | number]; change: [value: string | number] }>()
const root = ref<HTMLElement>(), menu = ref<HTMLElement>(), open = ref(false), activeIndex = ref(-1)
const selected = computed(() => props.options.find(option => option.value === props.modelValue))

function toggle() {
  open.value = !open.value
  if (open.value) {
    activeIndex.value = Math.max(0, props.options.findIndex(option => option.value === props.modelValue))
    nextTick(() => menu.value?.focus())
  }
}
function choose(option: SelectOption) {
  if (option.disabled) return
  emit('update:modelValue', option.value)
  emit('change', option.value)
  open.value = false
}
function keydown(event: KeyboardEvent) {
  if (event.key === 'Escape') { open.value = false; return }
  if (event.key === 'ArrowDown') { event.preventDefault(); activeIndex.value = Math.min(activeIndex.value + 1, props.options.length - 1) }
  if (event.key === 'ArrowUp') { event.preventDefault(); activeIndex.value = Math.max(activeIndex.value - 1, 0) }
  if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); const option = props.options[activeIndex.value]; if (option) choose(option) }
}
function outside(event: MouseEvent) { if (!root.value?.contains(event.target as Node)) open.value = false }
onMounted(() => document.addEventListener('mousedown', outside))
onBeforeUnmount(() => document.removeEventListener('mousedown', outside))
</script>

<template>
  <div ref="root" class="app-select" :class="{ open }">
    <button class="select-trigger" type="button" :aria-expanded="open" aria-haspopup="listbox" @click="toggle">
      <span :class="{ placeholder: !selected }">{{ selected?.label || placeholder || '请选择' }}</span>
      <ChevronDown :size="17" class="chevron" />
    </button>
    <Transition name="select-pop">
      <div v-if="open" ref="menu" class="select-menu" role="listbox" tabindex="-1" @keydown="keydown">
        <button v-for="(option,index) in options" :key="option.value" type="button" role="option"
          :aria-selected="option.value===modelValue" :disabled="option.disabled"
          :class="['select-option',{selected:option.value===modelValue,active:index===activeIndex}]"
          @mouseenter="activeIndex=index" @click="choose(option)">
          <span>{{ option.label }}</span><Check v-if="option.value===modelValue" :size="15" />
        </button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.app-select{position:relative;min-width:0}.select-trigger{width:100%;height:46px;display:flex;align-items:center;justify-content:space-between;gap:12px;padding:0 14px;border:1px solid var(--input);border-radius:calc(var(--radius)*.7);background:var(--background);color:var(--foreground);text-align:left;outline:0;transition:border .18s,box-shadow .18s,background .18s}.select-trigger:hover{background:color-mix(in oklch,var(--background) 80%,var(--secondary))}.open .select-trigger{border-color:var(--ring);box-shadow:0 0 0 3px color-mix(in oklch,var(--ring) 15%,transparent)}.placeholder{color:var(--muted-foreground)}.chevron{flex:0 0 auto;color:var(--muted-foreground);transition:transform .2s}.open .chevron{transform:rotate(180deg)}.select-menu{position:absolute;z-index:70;left:0;right:0;top:calc(100% + 7px);padding:6px;border:1px solid var(--border);border-radius:calc(var(--radius)*.75);background:color-mix(in oklch,var(--popover) 96%,transparent);box-shadow:0 18px 46px color-mix(in oklch,var(--foreground) 15%,transparent);backdrop-filter:blur(18px);outline:0}.select-option{width:100%;min-height:40px;display:flex;align-items:center;justify-content:space-between;gap:10px;padding:8px 10px;border:0;border-radius:calc(var(--radius)*.48);background:transparent;color:var(--popover-foreground);text-align:left;font-size:13px}.select-option:hover,.select-option.active{background:var(--accent);color:var(--accent-foreground)}.select-option.selected{font-weight:800;color:var(--primary)}.select-option.selected:hover,.select-option.selected.active{color:var(--accent-foreground)}.select-option:disabled{opacity:.4;cursor:not-allowed}.select-pop-enter-active,.select-pop-leave-active{transition:opacity .16s,transform .16s}.select-pop-enter-from,.select-pop-leave-to{opacity:0;transform:translateY(-5px) scale(.985)}
</style>

