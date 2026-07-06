import { ref, onUnmounted } from 'vue'
import { useDocumentStore } from '@/stores/document'
import { getDocumentList, getDocumentById, uploadDocument, updateDocumentStatus, deleteDocument, retryVector, getDocumentContent } from '@/api'
import type { Document, DocumentQueryParams } from '@/api/types'

export const useDocument = () => {
  const documentStore = useDocumentStore()
  const loading = ref(false)
  const pollingDocumentId = ref<number | null>(null)
  let pollingTimer: ReturnType<typeof setInterval> | null = null

  const fetchDocuments = async (params?: DocumentQueryParams) => {
    try {
      loading.value = true
      const data = await getDocumentList(params)
      documentStore.setDocuments(data)
      return data
    } finally {
      loading.value = false
    }
  }

  const fetchDocumentById = async (id: number) => {
    try {
      loading.value = true
      const data = await getDocumentById(id)
      documentStore.setCurrentDocument(data)
      return data
    } finally {
      loading.value = false
    }
  }

  const handleUploadDocument = async (file: File, documentName?: string) => {
    try {
      loading.value = true
      const data = await uploadDocument(file, documentName)
      documentStore.addDocument(data)
      startPolling(data.id)
      return data
    } finally {
      loading.value = false
    }
  }

  const handleUpdateStatus = async (id: number, status: number) => {
    try {
      loading.value = true
      const data = await updateDocumentStatus(id, status)
      documentStore.updateDocument(id, { status })
      return data
    } finally {
      loading.value = false
    }
  }

  const handleDeleteDocument = async (id: number) => {
    try {
      loading.value = true
      await deleteDocument(id)
      documentStore.removeDocument(id)
      if (pollingDocumentId.value === id) {
        stopPolling()
      }
    } finally {
      loading.value = false
    }
  }

  const handleRetryVector = async (id: number) => {
    try {
      loading.value = true
      await retryVector(id)
      documentStore.updateDocument(id, { vectorStatus: 'processing' })
      startPolling(id)
    } finally {
      loading.value = false
    }
  }

  const fetchDocumentContent = async (id: number) => {
    try {
      loading.value = true
      const content = await getDocumentContent(id)
      return content
    } finally {
      loading.value = false
    }
  }

  const startPolling = (documentId: number) => {
    stopPolling()
    pollingDocumentId.value = documentId
    pollingTimer = setInterval(async () => {
      try {
        const documentList = await getDocumentList()
        const updatedDoc = documentList.list.find(doc => doc.id === documentId)
        if (updatedDoc) {
          documentStore.updateDocument(documentId, {
            vectorStatus: updatedDoc.vectorStatus,
            chunkCount: updatedDoc.chunkCount,
            status: updatedDoc.status
          })
          if (updatedDoc.vectorStatus !== 'processing') {
            stopPolling()
          }
        }
      } catch (error) {
        console.error('轮询文档状态失败:', error)
      }
    }, 3000)
  }

  const stopPolling = () => {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
    pollingDocumentId.value = null
  }

  onUnmounted(() => {
    stopPolling()
  })

  return {
    documentStore,
    loading,
    pollingDocumentId,
    fetchDocuments,
    fetchDocumentById,
    fetchDocumentContent,
    handleUploadDocument,
    handleUpdateStatus,
    handleDeleteDocument,
    handleRetryVector,
    stopPolling
  }
}