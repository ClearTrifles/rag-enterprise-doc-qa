<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Download, View, Calendar, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useQA } from '@/hooks/useQA'

const { qaStore, loading, fetchHistory } = useQA()

const searchText = ref('')
const startDate = ref('')
const endDate = ref('')
const showDetailDialog = ref(false)
const currentDetail = ref<any>(null)

const filteredHistory = ref<any[]>([])

const handleSearch = () => {
  filteredHistory.value = qaStore.history.filter(item => {
    const matchText = !searchText.value || 
      item.question.toLowerCase().includes(searchText.value.toLowerCase()) ||
      item.answer.toLowerCase().includes(searchText.value.toLowerCase())
    return matchText
  })
}

const handleViewDetail = (item: any) => {
  currentDetail.value = item
  showDetailDialog.value = true
}

const handleExport = () => {
  const data = qaStore.history.map(item => ({
    question: item.question,
    answer: item.answer,
    timestamp: item.createdAt,
    sessionId: item.sessionId
  }))
  
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `qa_history_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('导出成功')
}

onMounted(() => {
  fetchHistory()
})
</script>

<template>
  <div class="history-container">
    <div class="page-header">
      <h2>问答记录</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Download" @click="handleExport">
          导出记录
        </el-button>
      </div>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchText"
        placeholder="搜索问题或答案..."
        class="search-input"
        @input="handleSearch"
        :prefix-icon="Search"
      />
      <el-date-picker
        v-model="startDate"
        type="date"
        placeholder="开始日期"
        format="YYYY-MM-DD"
      />
      <el-date-picker
        v-model="endDate"
        type="date"
        placeholder="结束日期"
        format="YYYY-MM-DD"
      />
      <el-button :icon="Search" @click="handleSearch">搜索</el-button>
    </div>
    
    <div class="history-list" :loading="loading">
      <div
        v-for="item in (searchText ? filteredHistory : qaStore.history)"
        :key="item.id"
        class="history-item"
      >
        <div class="item-header">
          <span class="session-tag">{{ item.sessionId }}</span>
          <span class="time">{{ item.createdAt }}</span>
        </div>
        <div class="item-content">
          <div class="question">
            <span class="label">问：</span>
            <span>{{ item.question }}</span>
          </div>
          <div class="answer">
            <span class="label">答：</span>
            <span :title="item.answer">{{ item.answer.length > 100 ? item.answer.substring(0, 100) + '...' : item.answer }}</span>
          </div>
          <div v-if="item.sources && item.sources.length > 0" class="sources">
            <span class="label">来源：</span>
            <el-tag v-for="src in item.sources.slice(0, 3)" :key="src.id" size="small">
              {{ src.documentName }}
            </el-tag>
            <span v-if="item.sources.length > 3" class="more-sources">等{{ item.sources.length }}个文档</span>
          </div>
        </div>
        <div class="item-actions">
          <el-button size="small" :icon="View" @click="handleViewDetail(item)">查看详情</el-button>
        </div>
      </div>
      
      <div v-if="!loading && (searchText ? filteredHistory.length : qaStore.history.length) === 0" class="empty-hint">
        <div class="hint-icon">
          <Search :size="48" />
        </div>
        <p>暂无问答记录</p>
      </div>
    </div>
    
    <div class="pagination">
      <el-pagination
        :total="qaStore.total"
        :page-size="qaStore.pageSize"
        :current-page="qaStore.pageNum"
        layout="prev, pager, next, jumper, ->, total"
        @current-change="(page) => fetchHistory({ pageNum: page, pageSize: qaStore.pageSize })"
      />
    </div>
    
    <el-dialog title="问答详情" v-model="showDetailDialog" width="600px">
      <div v-if="currentDetail" class="detail-content">
        <div class="detail-item">
          <label>会话ID</label>
          <span>{{ currentDetail.sessionId }}</span>
        </div>
        <div class="detail-item">
          <label>提问时间</label>
          <span>{{ currentDetail.createdAt }}</span>
        </div>
        <div class="detail-item">
          <label>置信度</label>
          <el-tag :type="currentDetail.confidence >= 0.7 ? 'success' : currentDetail.confidence >= 0.5 ? 'warning' : 'danger'">
            {{ (currentDetail.confidence * 100).toFixed(0) }}%
          </el-tag>
        </div>
        <div class="detail-item">
          <label>响应时间</label>
          <span>{{ currentDetail.responseTime }}ms</span>
        </div>
        <div class="detail-item">
          <label>问题</label>
          <p>{{ currentDetail.question }}</p>
        </div>
        <div class="detail-item">
          <label>回答</label>
          <p>{{ currentDetail.answer }}</p>
        </div>
        <div v-if="currentDetail.sources && currentDetail.sources.length > 0" class="detail-item">
          <label>引用来源</label>
          <div class="sources-detail">
            <div v-for="src in currentDetail.sources" :key="src.id" class="source-item">
              <span class="source-name">{{ src.documentName }}</span>
              <span class="source-page">第{{ src.pageNumber }}页</span>
              <p class="source-content">{{ src.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.history-container {
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
}

.search-input {
  width: 300px;
}

.history-list {
  max-height: calc(100vh - 400px);
  overflow-y: auto;
}

.history-item {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s;
}

.history-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.session-tag {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
}

.time {
  font-size: 12px;
  color: #94a3b8;
}

.item-content {
  margin-bottom: 12px;
}

.question, .answer {
  margin-bottom: 8px;
  line-height: 1.6;
}

.label {
  font-weight: 600;
  color: #64748b;
}

.sources {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.sources :deep(.el-tag) {
  background: #f0fdf4;
  border-color: #86efac;
  color: #166534;
}

.more-sources {
  font-size: 12px;
  color: #94a3b8;
}

.item-actions {
  text-align: right;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.hint-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-hint p {
  margin: 0;
  font-size: 14px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.detail-content {
  padding: 16px 0;
}

.detail-item {
  margin-bottom: 16px;
}

.detail-item label {
  display: block;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-item span {
  font-size: 14px;
  color: #334155;
}

.detail-item p {
  margin: 0;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
  color: #334155;
}

.sources-detail {
  margin-top: 8px;
}

.source-item {
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  margin-bottom: 8px;
}

.source-name {
  font-weight: 600;
  color: #334155;
  margin-right: 12px;
}

.source-page {
  font-size: 12px;
  color: #94a3b8;
}

.source-content {
  margin: 8px 0 0 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}
</style>