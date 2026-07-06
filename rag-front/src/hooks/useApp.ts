import { useAppStore } from '@/stores/app'

export const useApp = () => {
  const appStore = useAppStore()

  const toggleSidebar = () => {
    appStore.toggleSidebar()
  }

  const setCurrentPage = (page: string) => {
    appStore.setCurrentPage(page)
  }

  const setBreadcrumb = (items: Array<{ name: string; path: string }>) => {
    appStore.setBreadcrumb(items)
  }

  return {
    appStore,
    toggleSidebar,
    setCurrentPage,
    setBreadcrumb
  }
}