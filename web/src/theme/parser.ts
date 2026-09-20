import type { Theme } from '@/types'

export const allowedTokens = new Set([
  '--background','--foreground','--card','--card-foreground','--popover','--popover-foreground',
  '--primary','--primary-foreground','--secondary','--secondary-foreground','--muted','--muted-foreground',
  '--accent','--accent-foreground','--destructive','--border','--input','--ring',
  '--chart-1','--chart-2','--chart-3','--chart-4','--chart-5','--radius',
  '--sidebar','--sidebar-foreground','--sidebar-primary','--sidebar-primary-foreground','--sidebar-accent',
  '--sidebar-accent-foreground','--sidebar-border','--sidebar-ring','--todo','--todo-foreground','--diary',
  '--diary-foreground','--idea','--idea-foreground','--note','--note-foreground','--reminder',
  '--reminder-foreground','--success','--success-foreground','--overdue','--overdue-foreground',
])

const color=/^(oklch|hsl|hsla|rgb|rgba)\([^;{}]*\)$|^#[0-9a-f]{3,8}$|^(transparent|currentColor)$/i
const radius=/^0$|^[0-9]+(?:\.[0-9]+)?(?:px|rem|em)$/

export function parseThemeCss(css:string): Pick<Theme,'lightVariables'|'darkVariables'|'sourceCss'> {
  const sheet=new CSSStyleSheet()
  try{sheet.replaceSync(css)}catch{throw new Error('CSS 格式有误，无法解析')}
  const light:Record<string,string>={},dark:Record<string,string>={}
  for(const rule of sheet.cssRules){
    if(!(rule instanceof CSSStyleRule))throw new Error('只允许 :root 和 .dark 规则，不能使用 @import、@media 或 @font-face')
    if(rule.selectorText!==':root'&&rule.selectorText!=='.dark')throw new Error(`不允许选择器 ${rule.selectorText}`)
    const target=rule.selectorText===':root'?light:dark
    for(let i=0;i<rule.style.length;i++){
      const key=rule.style.item(i),value=rule.style.getPropertyValue(key).trim()
      if(!key.startsWith('--'))throw new Error(`只允许 CSS 变量，发现 ${key}`)
      if(!allowedTokens.has(key))throw new Error(`RecallHub 不支持变量 ${key}`)
      if(/url\(|image-set\(|expression\(/i.test(value))throw new Error(`${key} 包含不安全的值`)
      if(key==='--radius'?!radius.test(value):!color.test(value))throw new Error(`${key} 的值无法识别`)
      target[key]=value
    }
  }
  if(!Object.keys(light).length)throw new Error('没有找到 :root 主题变量')
  return {lightVariables:light,darkVariables:dark,sourceCss:css}
}

