<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)

const menuItems = [
  { path: '/chat', name: '智能问答' },
  { path: '/documents', name: '知识库管理' },
  { path: '/history', name: '问答记录' },
  { path: '/settings', name: '系统配置' }
]

const sidebarWidth = computed(() => collapsed.value ? '56px' : '180px')

const handleMenuClick = (path: string) => {
  router.push(path)
}
</script>

<template>
  <aside class="sidebar" :style="{ width: sidebarWidth }">
    <div class="logo-wrapper">
      <div class="logo">
        <div class="logo-icon">R</div>
        <span v-if="!collapsed" class="logo-text">RAG问答</span>
      </div>
    </div>
    
    <nav class="menu">
      <button
        v-for="item in menuItems"
        :key="item.path"
        class="menu-item"
        :class="{ active: route.path === item.path }"
        @click="handleMenuClick(item.path)"
      >
        <span class="menu-icon">{{ item.name.charAt(0) }}</span>
        <span v-if="!collapsed" class="menu-label">{{ item.name }}</span>
      </button>
    </nav>
    
    <button class="collapse-btn" @click="collapsed = !collapsed">
      <span>≡</span>
    </button>
  </aside>
</template>

<style scoped>
.sidebar {
  height: 100vh;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.logo-wrapper {
  padding: 12px;
  border-bottom: 1px solid #334155;
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  font-weight: 600;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.menu {
  flex: 1;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
  overflow-x: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  flex-shrink: 0;
}

.menu-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-item:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #e2e8f0;
}

.menu-item.active {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.menu-label {
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
}

.collapse-btn {
  padding: 10px;
  border-top: 1px solid #334155;
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #94a3b8;
}
</style>
