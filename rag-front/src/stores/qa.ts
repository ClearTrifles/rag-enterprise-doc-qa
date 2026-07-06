import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { QAAnswer, PageResult } from '@/api/types'

const SESSION_STORAGE_KEY = 'rag_current_session_id'
const MESSAGES_STORAGE_KEY = 'rag_session_messages'

export const useQAStore = defineStore('qa', () => {
  const currentSessionId = ref('')
  const history = ref<QAAnswer[]>([])
  const currentAnswer = ref<QAAnswer | null>(null)
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)

  const loadFromStorage = () => {
    try {
      const savedSessionId = localStorage.getItem(SESSION_STORAGE_KEY)
      if (savedSessionId) {
        currentSessionId.value = savedSessionId
      }
      
      const savedMessages = localStorage.getItem(`${MESSAGES_STORAGE_KEY}_${savedSessionId}`)
      if (savedMessages) {
        const parsed = JSON.parse(savedMessages)
        history.value = parsed.history || []
        total.value = parsed.total || 0
      }
    } catch (e) {
      console.error('Failed to load session from storage:', e)
    }
  }

  const saveToStorage = () => {
    try {
      if (currentSessionId.value) {
        localStorage.setItem(SESSION_STORAGE_KEY, currentSessionId.value)
        localStorage.setItem(`${MESSAGES_STORAGE_KEY}_${currentSessionId.value}`, JSON.stringify({
          history: history.value,
          total: total.value
        }))
      }
    } catch (e) {
      console.error('Failed to save session to storage:', e)
    }
  }

  const generateSessionId = () => {
    currentSessionId.value = `sess-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    history.value = []
    total.value = 0
    currentAnswer.value = null
    saveToStorage()
    return currentSessionId.value
  }

  const setSessionId = (sessionId: string) => {
    currentSessionId.value = sessionId
    saveToStorage()
  }

  const setHistory = (data: PageResult<QAAnswer>) => {
    history.value = data.list || []
    total.value = data.total || 0
    pageNum.value = data.pageNum || 1
    pageSize.value = data.pageSize || 10
    saveToStorage()
  }

  const addHistory = (answer: QAAnswer) => {
    history.value.unshift(answer)
    total.value++
    saveToStorage()
  }

  const updateHistory = (id: number, updates: Partial<QAAnswer>) => {
    const index = history.value.findIndex(item => item.id === id)
    if (index !== -1) {
      history.value[index] = { ...history.value[index], ...updates }
      saveToStorage()
    }
  }

  const setCurrentAnswer = (answer: QAAnswer | null) => {
    currentAnswer.value = answer
  }

  const clearHistory = () => {
    history.value = []
    total.value = 0
    currentAnswer.value = null
    if (currentSessionId.value) {
      localStorage.removeItem(`${MESSAGES_STORAGE_KEY}_${currentSessionId.value}`)
    }
  }

  loadFromStorage()

  watch([currentSessionId, history, total], () => {
    saveToStorage()
  }, { deep: true })

  return {
    currentSessionId,
    history,
    currentAnswer,
    total,
    pageNum,
    pageSize,
    generateSessionId,
    setSessionId,
    setHistory,
    addHistory,
    updateHistory,
    setCurrentAnswer,
    clearHistory
  }
})