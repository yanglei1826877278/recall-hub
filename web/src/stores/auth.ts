import { defineStore } from 'pinia'
import { api, unwrap } from '@/api/client'

interface AuthStatus { initialized: boolean; authenticated: boolean; username: string }

export const useAuthStore = defineStore('auth', {
  state: () => ({ initialized: false, authenticated: false, username: '', ready: false }),
  actions: {
    async check() {
      const status = await unwrap<AuthStatus>(api.get('/auth/status'))
      Object.assign(this, status, { ready: true })
      return status
    },
    async login(username: string, password: string) {
      await unwrap(api.post('/auth/login', { username, password }))
      await this.check()
    },
    async setup(username: string, password: string) {
      await unwrap(api.post('/auth/setup', { username, password }))
      await this.check()
    },
    async logout() {
      await api.post('/auth/logout')
      this.authenticated = false
      location.href = '/login'
    },
  },
})

