<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Edit, Trash2 } from '@element-plus/icons-vue'

const categories = ref([
  { id: 1, name: '技术文档', description: '技术相关文档', documentCount: 25, status: 'enabled' },
  { id: 2, name: '产品文档', description: '产品需求说明', documentCount: 18, status: 'enabled' },
  { id: 3, name: '管理文档', description: '管理制度文档', documentCount: 12, status: 'disabled' },
  { id: 4, name: '其他', description: '其他文档', documentCount: 5, status: 'enabled' }
])

const handleAdd = () => {
  alert('添加分类功能开发中')
}

const handleEdit = (id: number) => {
  alert(`编辑分类 ID: ${id}`)
}

const handleDelete = (id: number) => {
  categories.value = categories.value.filter(c => c.id !== id)
}

const handleToggleStatus = (id: number) => {
  const category = categories.value.find(c => c.id === id)
  if (category) {
    category.status = category.status === 'enabled' ? 'disabled' : 'enabled'
  }
}
</script>

<template>
  <div class="categories-container">
    <div class="page-header">
      <h2>分类管理</h2>
      <div class="header-actions">
        <el-button type="primary" icon="Plus" @click="handleAdd">
          添加分类
        </el-button>
      </div>
    </div>
    
    <div class="categories-table">
      <el-table :data="categories" border>
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="documentCount" label="文档数量" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-switch
              :value="scope.row.status === 'enabled'"
              @change="handleToggleStatus(scope.row.id)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" icon="Edit" @click="handleEdit(scope.row.id)">编辑</el-button>
            <el-button size="small" icon="Delete" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.categories-container {
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
</style>