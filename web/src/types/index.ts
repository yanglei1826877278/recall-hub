export type EntryType = 'TODO' | 'DIARY' | 'NOTE' | 'IDEA'
export type EntryStatus = 'ACTIVE' | 'DONE' | 'ARCHIVED'

export interface Entry {
  id: number
  type: EntryType
  title?: string
  content?: string
  status: EntryStatus
  occurredAt?: string
  dueAt?: string
  completedAt?: string
  reminderStatus?: 'SCHEDULED' | 'PROCESSING' | 'SENT' | 'FAILED' | 'CANCELLED'
  remindAt?: string
  reminderSentAt?: string
  createdAt: string
  updatedAt: string
}

export interface Reminder {
  id: number
  entryId: number
  notificationTargetId?: number
  remindAt: string
  status: string
  attemptCount: number
  nextAttemptAt?: string
  lastError?: string
}

export interface Theme {
  id: number
  name: string
  slug: string
  lightVariables: Record<string, string>
  darkVariables: Record<string, string>
  sourceCss?: string
  builtin: boolean
}

export interface TodayData {
  date: string
  overdue: Entry[]
  reminders: Reminder[]
  todos: Entry[]
  entries: Entry[]
  journal: Record<string, unknown>
}
