#!/usr/bin/env node

import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import { homedir } from 'node:os'
import { join } from 'node:path'

const configPath = process.env.RECALLHUB_CONFIG || join(homedir(), '.config', 'recallhub', 'config.json')
let fileConfig = {}
try {
  fileConfig = JSON.parse(readFileSync(configPath, 'utf8'))
} catch (error) {
  if (error.code !== 'ENOENT') fail(`配置文件无法读取：${configPath} (${error.message})`)
}

const base = (process.env.RECALLHUB_URL || fileConfig.url || 'http://127.0.0.1:8080').replace(/\/$/, '')
const token = process.env.RECALLHUB_TOKEN || fileConfig.token
if (!token) fail('RECALLHUB_TOKEN 未配置')

const [command, ...args] = process.argv.slice(2)
const keyIndex = args.indexOf('--key')
const explicitKey = keyIndex >= 0 ? args[keyIndex + 1] : undefined
if (keyIndex >= 0) args.splice(keyIndex, 2)

async function readStdin() {
  let text = ''
  for await (const chunk of process.stdin) text += chunk
  if (!text.trim()) fail('需要通过标准输入提供 JSON')
  try { return JSON.parse(text) } catch { fail('标准输入不是有效 JSON') }
}

async function request(method, path, body, idempotencyKey) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 20_000)
  try {
    const response = await fetch(`${base}/api/v1${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })
    const text = await response.text()
    let value
    try { value = text ? JSON.parse(text) : {} } catch { value = { message: text } }
    if (!response.ok) fail(`${value.message || 'RecallHub 请求失败'} (HTTP ${response.status})`)
    return value.data
  } catch (error) {
    if (error.name === 'AbortError') fail('RecallHub 请求超时')
    fail(error.message)
  } finally { clearTimeout(timer) }
}

function autoKey(body) {
  return `openclaw:auto:${createHash('sha256').update(JSON.stringify(body)).digest('hex').slice(0, 32)}`
}
function print(value) { process.stdout.write(`${JSON.stringify(value, null, 2)}\n`) }
function fail(message) { process.stderr.write(`RecallHub: ${message}\n`); process.exit(1) }

switch (command) {
  case 'capture': { const body = await readStdin(); print(await request('POST', '/captures', body, explicitKey || autoKey(body))); break }
  case 'batch': { const body = await readStdin(); print(await request('POST', '/captures/batch', body, explicitKey || autoKey(body))); break }
  case 'search': print(await request('GET', `/search?q=${encodeURIComponent(args[0] || '')}`)); break
  case 'today': print(await request('GET', '/today')); break
  case 'reminders': print(await request('GET', '/reminders')); break
  case 'complete': print(await request('POST', `/entries/${Number(args[0])}/complete`)); break
  case 'snooze': print(await request('POST', `/reminders/${Number(args[0])}/snooze`, { until: args[1] })); break
  case 'cancel-reminder': print(await request('POST', `/reminders/${Number(args[0])}/cancel`)); break
  default: fail('命令应为 capture、batch、search、today、reminders、complete、snooze 或 cancel-reminder')
}
