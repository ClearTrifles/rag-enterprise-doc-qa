import { request } from '@/utils/request'
import type { Document, PageResult, DocumentQueryParams } from './types'

export const uploadDocument = (file: File, documentName?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  if (documentName) {
    formData.append('documentName', documentName)
  }
  return request.post<Document>('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    loading: true
  })
}

export const getDocumentList = (params: DocumentQueryParams = {}) => {
  return request.get<PageResult<Document>>('/documents', { params, loading: true })
}

export const getDocumentById = (id: number) => {
  return request.get<Document>(`/documents/${id}`, { loading: true })
}

export const updateDocumentStatus = (id: number, status: number) => {
  return request.put<Document>(`/documents/${id}/status`, null, {
    params: { status },
    loading: true
  })
}

export const deleteDocument = (id: number) => {
  return request.delete(`/documents/${id}`, { loading: true })
}

export const retryVector = (id: number) => {
  return request.put(`/documents/${id}/retry-vector`, null, { loading: true })
}

export const getDocumentContent = (id: number) => {
  return request.get<string>(`/documents/${id}/content`, { loading: true })
}