import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppShell from '@/layouts/AppShell.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/pages/LoginPage.vue') },
    { path: '/s/:token', name: 'journal-share', component: () => import('@/pages/PublicJournalSharePage.vue'), meta: { title: '一页日记', public: true } },
    { path: '/journal/:date/read', name: 'journal-reader', component: () => import('@/pages/ImmersiveJournalPage.vue'), meta: { title: '沉浸阅读' } },
    {
      path: '/', component: AppShell,
      children: [
        { path: '', name: 'today', component: () => import('@/pages/TodayPage.vue'), meta: { title: '今天' } },
        { path: 'timeline', name: 'timeline', component: () => import('@/pages/TimelinePage.vue'), meta: { title: '时间线' } },
        { path: 'journal', name: 'journal', component: () => import('@/pages/JournalPage.vue'), meta: { title: '日记' } },
        { path: 'search', name: 'search', component: () => import('@/pages/SearchPage.vue'), meta: { title: '搜索' } },
        { path: 'settings', name: 'settings', component: () => import('@/pages/SettingsPage.vue'), meta: { title: '设置' } },
      ],
    },
  ],
})

router.beforeEach(async to => {
  if (to.meta.public) return
  const auth = useAuthStore()
  if (!auth.ready) { try { await auth.check() } catch { /* handled below */ } }
  if (to.path !== '/login' && !auth.authenticated) return '/login'
  if (to.path === '/login' && auth.authenticated) return '/'
})

router.afterEach(to => { document.title = `${to.meta.title || 'RecallHub'} · RecallHub` })
export default router
