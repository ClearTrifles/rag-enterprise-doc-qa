import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Document, PageResult } from '@/api/types'

export const useDocumentStore = defineStore('document', () => {
  const documents = ref<Document[]>([])
  const currentDocument = ref<Document | null>(null)
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)

  const setDocuments = (data: PageResult<Document>) => {
    documents.value = data.list || []
    total.value = data.total || 0
    pageNum.value = data.pageNum || 1
    pageSize.value = data.pageSize || 10
  }

  const setCurrentDocument = (document: Document | null) => {
    currentDocument.value = document
  }

  const addDocument = (document: Document) => {
    documents.value.unshift(document)
    total.value++
  }

  const updateDocument = (id: number, updates: Partial<Document>) => {
    const index = documents.value.findIndex(doc => doc.id === id)
    if (index !== -1) {
      documents.value[index] = { ...documents.value[index], ...updates }
    }
  }

  const removeDocument = (id: number) => {
    const index = documents.value.findIndex(doc => doc.id === id)
    if (index !== -1) {
      documents.value.splice(index, 1)
      total.value--
    }
  }

  return {
    documents,
    currentDocument,
    total,
    pageNum,
    pageSize,
    setDocuments,
    setCurrentDocument,
    addDocument,
    updateDocument,
    removeDocument
  }
})