import { defineStore } from 'pinia'
import { api, unwrap } from '@/api/client'
import type { Theme } from '@/types'

const fallback: Record<string, string> = {
  '--todo': 'oklch(0.9 0.055 245)', '--todo-foreground': 'oklch(0.35 0.12 250)',
  '--diary': 'oklch(0.92 0.045 160)', '--diary-foreground': 'oklch(0.35 0.09 160)',
  '--idea': 'oklch(0.93 0.065 90)', '--idea-foreground': 'oklch(0.4 0.1 75)',
  '--note': 'oklch(0.92 0.04 310)', '--note-foreground': 'oklch(0.4 0.09 305)',
  '--reminder': 'oklch(0.92 0.06 45)', '--reminder-foreground': 'oklch(0.42 0.14 35)',
  '--success': 'oklch(0.72 0.14 155)', '--success-foreground': 'oklch(0.2 0.06 155)',
  '--overdue': 'oklch(0.9 0.07 25)', '--overdue-foreground': 'oklch(0.48 0.18 25)',
}

function writeStyle(theme: Theme) {
  let style = document.querySelector<HTMLStyleElement>('#recallhub-theme')
  if (!style) { style = document.createElement('style'); style.id = 'recallhub-theme'; document.head.append(style) }
  const block = (selector: string, values: Record<string, string>) =>
    `${selector}{${Object.entries({ ...fallback, ...values }).map(([k, v]) => `${k}:${v}`).join(';')}}`
  style.textContent = `${block(':root', theme.lightVariables)}${block('.dark', theme.darkVariables)}`
}

export const useThemeStore = defineStore('theme', {
  state: () => ({ mode: (localStorage.getItem('appearance') || 'SYSTEM') as 'LIGHT' | 'DARK' | 'SYSTEM', current: null as Theme | null }),
  actions: {
    async initialize() {
      this.applyMode()
      try { this.current = await unwrap<Theme>(api.get('/themes/current')); writeStyle(this.current) } catch { /* login page */ }
      matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => this.applyMode())
    },
    applyMode() {
      const dark = this.mode === 'DARK' || (this.mode === 'SYSTEM' && matchMedia('(prefers-color-scheme: dark)').matches)
      document.documentElement.classList.toggle('dark', dark)
      document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
      localStorage.setItem('appearance', this.mode)
    },
    setMode(mode: 'LIGHT' | 'DARK' | 'SYSTEM') { this.mode = mode; this.applyMode() },
    preview(theme: Theme) { writeStyle(theme) },
    async activate(id: number) { this.current = await unwrap<Theme>(api.post(`/themes/${id}/activate`)); writeStyle(this.current) },
  },
})

