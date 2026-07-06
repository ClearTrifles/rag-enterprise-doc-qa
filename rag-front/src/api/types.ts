export interface Document {
  id: number
  documentName: string
  fileName: string
  fileType: string
  fileSize: number
  status: number
  uploadTime: string
  chunkCount: number
  vectorStatus: string
  storagePath?: string
  createdAt?: string
  updatedAt?: string
}

export interface Source {
  id: number
  documentId: number
  documentName: string
  pageNumber: number
  content: string
}

export interface QAAnswer {
  id: number
  question: string
  answer: string
  sessionId: string
  sources?: Source[]
  confidence?: number
  responseTime?: number
  createdAt?: string
  timestamp?: number
  source?: string
  status?: string
  replyType?: string
  referenceList?: Array<{
    documentId: number
    documentName?: string
    content: string
    score?: number
  }>
  promptTip?: string
}

export interface PageResult<T = any> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages?: number
}

export interface AskRequest {
  question: string
  sessionId?: string
}

export interface DocumentQueryParams {
  documentName?: string
  status?: number
  fileType?: string
  pageNum?: number
  pageSize?: number
}

export interface QAHistoryParams {
  sessionId?: string
  pageNum?: number
  pageSize?: number
}