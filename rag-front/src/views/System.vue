<script setup lang="ts">
import { ref } from 'vue'

const systemSettings = ref([
  { key: '服务端口', value: '8080', editable: false },
  { key: '数据库类型', value: 'MySQL', editable: false },
  { key: '向量数据库', value: 'ChromaDB', editable: false },
  { key: 'AI模型', value: 'Qwen Plus', editable: false },
  { key: '向量维度', value: '1024', editable: false },
  { key: '相似度阈值', value: '0.75', editable: true },
  { key: '召回条数', value: '5', editable: true },
  { key: '切片大小', value: '512', editable: true }
])

const handleSave = () => {
  alert('系统配置已保存')
}
</script>

<template>
  <div class="system-container">
    <div class="page-header">
      <h2>系统配置</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </div>
    </div>
    
    <div class="settings-grid">
      <div class="setting-card">
        <h3>服务配置</h3>
        <div v-for="setting in systemSettings.slice(0, 5)" :key="setting.key" class="setting-row">
          <span class="label">{{ setting.key }}</span>
          <span class="value">{{ setting.value }}</span>
        </div>
      </div>
      
      <div class="setting-card">
        <h3>运行参数</h3>
        <div v-for="setting in systemSettings.slice(5)" :key="setting.key" class="setting-row">
          <span class="label">{{ setting.key }}</span>
          <el-input 
            v-if="setting.editable" 
            :value="setting.value" 
            class="editable-input"
          />
          <span v-else class="value">{{ setting.value }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.system-container {
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

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.setting-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 20px;
}

.setting-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e2e8f0;
}

.setting-row:last-child {
  border-bottom: none;
}

.label {
  font-size: 14px;
  color: #475569;
}

.value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.editable-input {
  width: 120px;
}
</style>