import { createRouter } from 'vue-router'
import type { RouteRecordRaw, RouterHistory } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue'),
    meta: { title: '智能问答', showMenu: true }
  },
  {
    path: '/documents',
    name: 'Documents',
    component: () => import('@/views/Documents.vue'),
    meta: { title: '知识库管理', showMenu: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/History.vue'),
    meta: { title: '问答记录', showMenu: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { title: '个人设置', showMenu: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面未找到', showMenu: false }
  }
]

function createCustomHashHistory(): RouterHistory {
  const currentPath = { value: window.location.hash.slice(1) || '/' }
  const listeners: ((to: string, from: string, info: { type: string; direction: string; delta: number }) => void)[] = []
  
  function handleHashChange() {
    const newPath = window.location.hash.slice(1) || '/'
    if (newPath !== currentPath.value) {
      const oldPath = currentPath.value
      currentPath.value = newPath
      listeners.forEach(listener => listener(newPath, oldPath, {
        type: 'push',
        direction: 'forward',
        delta: 1
      }))
    }
  }
  
  window.addEventListener('hashchange', handleHashChange)
  
  // 初始化时触发一次hashchange，确保路由正确加载
  if (window.location.hash) {
    handleHashChange()
  }
  
  return {
    get location() {
      return {
        pathname: currentPath.value,
        search: '',
        hash: '',
        href: window.location.href,
        origin: window.location.origin,
        protocol: window.location.protocol,
        host: window.location.host,
        hostname: window.location.hostname,
        port: window.location.port
      }
    },
    get state() {
      return {
        back: null,
        current: currentPath.value,
        forward: null,
        replaced: false,
        position: window.history.length,
        scroll: null,
        delta: 0
      }
    },
    push(to: string) {
      window.location.hash = to
    },
    replace(to: string) {
      const currentUrl = window.location.href
      const hashIndex = currentUrl.indexOf('#')
      const baseUrl = hashIndex > -1 ? currentUrl.slice(0, hashIndex) : currentUrl
      window.location.replace(baseUrl + '#' + to)
    },
    go: (delta: number) => window.history.go(delta),
    back: () => window.history.back(),
    forward: () => window.history.forward(),
    listen(callback: (to: string, from: string, info: { type: string; direction: string; delta: number }) => void) {
      listeners.push(callback)
      return () => {
        const index = listeners.indexOf(callback)
        if (index > -1) listeners.splice(index, 1)
      }
    },
    createHref(to: string) {
      return '#' + to
    },
    destroy() {
      window.removeEventListener('hashchange', handleHashChange)
    }
  }
}

const router = createRouter({
  history: createCustomHashHistory(),
  routes
})

export default router
