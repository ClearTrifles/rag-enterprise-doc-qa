<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface AppSettings {
  similarityThreshold: number
  recallTopNum: number
  chunkSize: number
  chunkOverlap: number
  temperature: number
  maxTokens: number
  modelName: string
  apiTimeout: number
}

const settings = reactive<AppSettings>({
  similarityThreshold: 0.75,
  recallTopNum: 5,
  chunkSize: 512,
  chunkOverlap: 120,
  temperature: 0.7,
  maxTokens: 2048,
  modelName: 'qwen-plus',
  apiTimeout: 60000
})

const originalSettings = { ...settings }
const saving = ref(false)

const modelOptions = [
  { label: 'qwen-plus', value: 'qwen-plus' },
  { label: 'qwen-turbo', value: 'qwen-turbo' },
  { label: 'qwen3.6-flash', value: 'qwen3.6-flash' },
  { label: 'qwen3.6-max', value: 'qwen3.6-max' }
]

const handleSave = async () => {
  saving.value = true
  
  await new Promise(resolve => setTimeout(resolve, 800))
  
  Object.assign(originalSettings, settings)
  saving.value = false
  
  ElMessage.success('设置已保存')
}

const handleReset = () => {
  Object.assign(settings, originalSettings)
  ElMessage.info('已恢复默认设置')
}

const handleExport = () => {
  const blob = new Blob([JSON.stringify(settings, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `settings_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('配置导出成功')
}
</script>

<template>
  <div class="settings-container">
    <div class="page-header">
      <h2>系统配置</h2>
      <div class="header-actions">
        <el-button :icon="Download" @click="handleExport">导出配置</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存设置</el-button>
      </div>
    </div>
    
    <div class="settings-content">
      <div class="setting-section">
        <h3>
          <span class="section-icon">🔍</span>
          检索参数
        </h3>
        <div class="setting-item">
          <label>相似度阈值</label>
          <div class="input-control">
            <el-input-number
              v-model="settings.similarityThreshold"
              :min="0"
              :max="1"
              :step="0.01"
              class="input-number"
            />
            <div class="range-bar">
              <el-slider
                v-model="settings.similarityThreshold"
                :min="0"
                :max="1"
                :step="0.01"
                style="width: 200px;"
              />
            </div>
          </div>
          <span class="description">用于判断检索结果有效性，低于此阈值时拒绝回答</span>
        </div>
        <div class="setting-item">
          <label>召回条数</label>
          <el-input-number
            v-model="settings.recallTopNum"
            :min="1"
            :max="20"
            class="input-number"
          />
          <span class="description">从知识库中召回的相关文档数量</span>
        </div>
      </div>
      
      <div class="setting-section">
        <h3>
          <span class="section-icon">📄</span>
          文档切片参数
        </h3>
        <div class="setting-item">
          <label>切片大小</label>
          <el-input-number
            v-model="settings.chunkSize"
            :min="128"
            :max="2048"
            :step="128"
            class="input-number"
          />
          <span class="description">每个文档切片的字符数，建议512-1024</span>
        </div>
        <div class="setting-item">
          <label>切片重叠</label>
          <el-input-number
            v-model="settings.chunkOverlap"
            :min="0"
            :max="512"
            :step="16"
            class="input-number"
          />
          <span class="description">相邻切片之间的重叠字符数，保持上下文连贯性</span>
        </div>
      </div>
      
      <div class="setting-section">
        <h3>
          <span class="section-icon">🤖</span>
          模型参数
        </h3>
        <div class="setting-item">
          <label>模型选择</label>
          <el-select v-model="settings.modelName" class="input-select">
            <el-option v-for="model in modelOptions" :key="model.value" :label="model.label" :value="model.value" />
          </el-select>
          <span class="description">选择使用的AI模型</span>
        </div>
        <div class="setting-item">
          <label>温度系数</label>
          <div class="input-control">
            <el-input-number
              v-model="settings.temperature"
              :min="0"
              :max="1"
              :step="0.1"
              class="input-number"
            />
            <div class="range-bar">
              <el-slider
                v-model="settings.temperature"
                :min="0"
                :max="1"
                :step="0.1"
                style="width: 200px;"
              />
            </div>
          </div>
          <span class="description">控制回答的随机性，值越高越随机（0-1）</span>
        </div>
        <div class="setting-item">
          <label>最大Token数</label>
          <el-input-number
            v-model="settings.maxTokens"
            :min="256"
            :max="8192"
            :step="256"
            class="input-number"
          />
          <span class="description">生成回答的最大Token数量</span>
        </div>
        <div class="setting-item">
          <label>API超时时间</label>
          <el-input-number
            v-model="settings.apiTimeout"
            :min="10000"
            :max="300000"
            :step="10000"
            class="input-number"
          />
          <span class="description">API请求超时时间（毫秒）</span>
        </div>
      </div>
      
      <div class="setting-section">
        <h3>
          <span class="section-icon">ℹ️</span>
          配置说明
        </h3>
        <div class="info-content">
          <ul>
            <li><strong>相似度阈值</strong>：推荐设置0.7-0.8，值越高检索结果越严格</li>
            <li><strong>召回条数</strong>：建议3-10条，过多会增加响应时间</li>
            <li><strong>切片大小</strong>：中文建议512-1024字符，英文建议1024-2048</li>
            <li><strong>温度系数</strong>：0表示确定性回答，1表示完全随机</li>
            <li><strong>最大Token</strong>：根据回答长度需求调整，越大响应越慢</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-container {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.settings-content {
  max-width: 800px;
}

.setting-section {
  margin-bottom: 32px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 8px;
}

.setting-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 18px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-item label {
  width: 120px;
  font-weight: 500;
  color: #475569;
  flex-shrink: 0;
}

.input-control {
  display: flex;
  align-items: center;
  gap: 16px;
}

.input-number {
  width: 150px;
}

.input-select {
  width: 200px;
}

.range-bar {
  flex-shrink: 0;
}

.description {
  flex: 1;
  font-size: 13px;
  color: #94a3b8;
}

.info-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
}

.info-content ul {
  margin: 0;
  padding-left: 20px;
}

.info-content li {
  margin-bottom: 8px;
}

.info-content li:last-child {
  margin-bottom: 0;
}
</style>