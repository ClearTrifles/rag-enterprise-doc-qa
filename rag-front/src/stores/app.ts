import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentPage = ref('')
  const breadcrumb = ref<Array<{ name: string; path: string }>>([])

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setCurrentPage = (page: string) => {
    currentPage.value = page
  }

  const setBreadcrumb = (items: Array<{ name: string; path: string }>) => {
    breadcrumb.value = items
  }

  return {
    sidebarCollapsed,
    currentPage,
    breadcrumb,
    toggleSidebar,
    setCurrentPage,
    setBreadcrumb
  }
})