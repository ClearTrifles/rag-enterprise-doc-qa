<script setup lang="ts">
import { ref } from 'vue'
import { Download, Search } from '@element-plus/icons-vue'

const logs = ref([
  { id: 1, time: '2024-01-15 14:30:25', user: '管理员', action: '登录系统', ip: '192.168.1.100' },
  { id: 2, time: '2024-01-15 14:25:18', user: '管理员', action: '上传文档', ip: '192.168.1.100' },
  { id: 3, time: '2024-01-15 10:15:42', user: '张三', action: '查询文档列表', ip: '192.168.1.101' },
  { id: 4, time: '2024-01-14 16:40:33', user: '李四', action: '智能问答', ip: '192.168.1.102' },
  { id: 5, time: '2024-01-14 11:20:08', user: '管理员', action: '删除文档', ip: '192.168.1.100' }
])

const searchText = ref('')

const filteredLogs = ref(logs.value)

const handleSearch = () => {
  filteredLogs.value = logs.value.filter(log => 
    log.user.includes(searchText.value) || 
    log.action.includes(searchText.value)
  )
}

const handleExport = () => {
  alert('导出日志功能开发中')
}
</script>

<template>
  <div class="logs-container">
    <div class="page-header">
      <h2>操作日志</h2>
      <div class="header-actions">
        <el-button type="primary" icon="Download" @click="handleExport">
          导出日志
        </el-button>
      </div>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchText"
        placeholder="搜索用户或操作..."
        class="search-input"
        @input="handleSearch"
      />
    </div>
    
    <div class="logs-table">
      <el-table :data="filteredLogs" border>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="user" label="操作人" />
        <el-table-column prop="action" label="操作描述" />
        <el-table-column prop="ip" label="IP地址" />
      </el-table>
    </div>
    
    <div class="pagination">
      <el-pagination
        :total="filteredLogs.length"
        :page-size="10"
        layout="prev, pager, next"
      />
    </div>
  </div>
</template>

<style scoped>
.logs-container {
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
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style>