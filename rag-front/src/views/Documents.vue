<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Upload, View, Refresh, Delete, Search, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDocument } from '@/hooks/useDocument'
import type { Document } from '@/api/types'

const { documentStore, loading, pollingDocumentId, fetchDocuments, handleUploadDocument, handleUpdateStatus, handleDeleteDocument, handleRetryVector: retryVectorAction, fetchDocumentContent } = useDocument()

const searchText = ref('')
const statusFilter = ref('all')
const fileTypeFilter = ref('all')
const showUploadDialog = ref(false)
const showViewDialog = ref(false)
const viewContent = ref('')
const viewDocumentName = ref('')
const selectedFile = ref<File | null>(null)
const documentName = ref('')

const fileTypes = ['all', 'pdf', 'docx', 'md', 'txt']

const handleSearch = () => {
  fetchDocuments({
    pageNum: 1,
    pageSize: documentStore.pageSize,
    documentName: searchText.value || undefined,
    status: statusFilter.value === 'all' ? undefined : parseInt(statusFilter.value),
    fileType: fileTypeFilter.value === 'all' ? undefined : fileTypeFilter.value
  })
}

const resetSearch = () => {
  searchText.value = ''
  statusFilter.value = 'all'
  fileTypeFilter.value = 'all'
  handleSearch()
}

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    selectedFile.value = target.files[0]
    if (!documentName.value) {
      documentName.value = selectedFile.value.name.replace(/\.[^/.]+$/, '')
    }
  }
}

const submitUpload = async () => {
  if (!selectedFile.value) return
  
  try {
    await handleUploadDocument(selectedFile.value, documentName.value)
    ElMessage.success('文件上传成功')
    showUploadDialog.value = false
    selectedFile.value = null
    documentName.value = ''
  } catch (error) {
    ElMessage.error('文件上传失败')
  }
}

const handleStatusChange = async (doc: Document) => {
  try {
    const newStatus = doc.status === 2 ? 3 : 2
    await handleUpdateStatus(doc.id, newStatus)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

const handleStatusChangeDirect = async (newStatus: number, doc: Document) => {
  try {
    await handleUpdateStatus(doc.id, newStatus)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
    doc.status = newStatus === 2 ? 3 : 2
  }
}

const handleView = async (id: number) => {
  try {
    viewContent.value = await fetchDocumentContent(id)
    const doc = documentStore.documents.find(d => d.id === id)
    viewDocumentName.value = doc?.documentName || '文档详情'
    showViewDialog.value = true
  } catch (error) {
    ElMessage.error('查看失败')
  }
}

const handleRetryVector = async (id: number) => {
  try {
    await retryVectorAction(id)
    ElMessage.success('已重新触发向量化，正在处理中...')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id: number) => {
  ElMessageBox.confirm(
    '确定要删除该文档吗？此操作不可恢复。',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await handleDeleteDocument(id)
      ElMessage.success('删除成功')
      fetchDocuments()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(() => {
  fetchDocuments()
})
</script>

<template>
  <div class="documents-wrapper">
    <div class="documents-container">
    <div class="page-header">
      <h2>知识库管理</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">
          上传文档
        </el-button>
        <el-button :icon="Refresh" @click="fetchDocuments">
          刷新
        </el-button>
      </div>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchText"
        placeholder="搜索文档名称..."
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="statusFilter" placeholder="状态筛选">
        <el-option label="全部" value="all" />
        <el-option label="启用" :value="2" />
        <el-option label="禁用" :value="3" />
      </el-select>
      <el-select v-model="fileTypeFilter" placeholder="文件类型">
        <el-option v-for="type in fileTypes" :key="type" :label="type === 'all' ? '全部' : type.toUpperCase()" :value="type" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>
    
    <div class="documents-table">
      <el-table 
        :data="documentStore.documents" 
        :loading="loading"
        empty-text="暂无文档数据"
        :fit="true"
        class="document-table"
      >
        <el-table-column prop="documentName" label="文档名称" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <div class="doc-name">
              <span class="doc-icon">{{ scope.row.fileType.toUpperCase() }}</span>
              <span class="doc-name-text">{{ scope.row.documentName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="90">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.fileType.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="90">
          <template #default="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数量" width="90" />
        <el-table-column prop="vectorStatus" label="向量化状态" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.vectorStatus === 'completed' ? 'success' : scope.row.vectorStatus === 'processing' ? 'warning' : 'info'">
              <span v-if="scope.row.vectorStatus === 'processing' && pollingDocumentId === scope.row.id" class="processing-text">
                <el-icon class="is-loading"><Loading /></el-icon>
                处理中
              </span>
              <span v-else>
                {{ scope.row.vectorStatus === 'completed' ? '已完成' : scope.row.vectorStatus === 'processing' ? '处理中' : '未开始' }}
              </span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="2"
              :inactive-value="3"
              @change="(val: number) => handleStatusChangeDirect(val, scope.row)"
              :loading="loading"
            />
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="140" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" :icon="View" @click="handleView(scope.row.id)">查看</el-button>
            <el-button 
              size="small" 
              :type="scope.row.status === 2 ? 'warning' : 'success'" 
              @click="handleStatusChange(scope.row)"
            >
              {{ scope.row.status === 2 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" :icon="Refresh" @click="handleRetryVector(scope.row.id)">重试向量</el-button>
            <el-button size="small" :icon="Delete" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <div class="pagination">
      <el-pagination
        :total="documentStore.total"
        :page-size="documentStore.pageSize"
        :current-page="documentStore.pageNum"
        layout="prev, pager, next, jumper, ->, total"
        @current-change="(page) => fetchDocuments({ pageNum: page, pageSize: documentStore.pageSize })"
      />
    </div>
    
    <el-dialog title="上传文档" v-model="showUploadDialog" width="400px">
      <div class="upload-form">
        <el-form :model="{ documentName }" label-width="100px">
          <el-form-item label="文档名称">
            <el-input v-model="documentName" placeholder="请输入文档名称" />
          </el-form-item>
          <el-form-item label="选择文件">
            <input type="file" class="file-input" @change="handleFileChange" accept=".pdf,.docx,.md,.txt" />
            <div v-if="selectedFile" class="file-info">
              <span>{{ selectedFile.name }}</span>
              <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :disabled="!selectedFile">上传</el-button>
      </template>
    </el-dialog>
    
    <el-dialog :title="viewDocumentName" v-model="showViewDialog" width="800px" top="5vh">
      <div class="view-content">
        <pre>{{ viewContent }}</pre>
      </div>
      <template #footer>
        <el-button @click="showViewDialog = false">关闭</el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.documents-wrapper {
  width: 100%;
  overflow-x: hidden;
}

.documents-container {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-input {
  width: 300px;
}

.documents-table {
  overflow-x: auto;
}

.documents-table :deep(.el-table) {
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f8fafc;
  width: 100% !important;
  table-layout: fixed;
}

.documents-table :deep(.el-table__body-wrapper),
.documents-table :deep(.el-table__header-wrapper) {
  overflow-x: auto;
}

.doc-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.doc-name-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-icon {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #fff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.upload-form {
  padding: 20px 0;
}

.file-input {
  display: block;
  width: 100%;
  padding: 12px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  background: #fafafa;
  transition: all 0.2s;
}

.file-input:hover {
  border-color: #3b82f6;
  background: #f0f9ff;
}

.file-info {
  margin-top: 12px;
  font-size: 14px;
  color: #64748b;
}

.file-size {
  color: #94a3b8;
  margin-left: 8px;
}

.view-content {
  max-height: 500px;
  overflow-y: auto;
}

.view-content pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: #334155;
  margin: 0;
}

.processing-text {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>