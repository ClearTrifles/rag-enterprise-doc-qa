<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Edit, Trash2, Lock, Unlock } from '@element-plus/icons-vue'

const users = ref([
  { id: 1, name: '管理员', email: 'admin@example.com', role: '管理员', status: 'active' },
  { id: 2, name: '张三', email: 'zhangsan@example.com', role: '普通用户', status: 'active' },
  { id: 3, name: '李四', email: 'lisi@example.com', role: '普通用户', status: 'inactive' }
])

const handleAdd = () => {
  alert('添加用户功能开发中')
}

const handleEdit = (id: number) => {
  alert(`编辑用户 ID: ${id}`)
}

const handleDelete = (id: number) => {
  users.value = users.value.filter(u => u.id !== id)
}

const handleToggleStatus = (id: number) => {
  const user = users.value.find(u => u.id === id)
  if (user) {
    user.status = user.status === 'active' ? 'inactive' : 'active'
  }
}
</script>

<template>
  <div class="users-container">
    <div class="page-header">
      <h2>权限管理</h2>
      <div class="header-actions">
        <el-button type="primary" icon="Plus" @click="handleAdd">
          添加用户
        </el-button>
      </div>
    </div>
    
    <div class="users-table">
      <el-table :data="users" border>
        <el-table-column prop="name" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色">
          <template #default="scope">
            <el-tag :type="scope.row.role === '管理员' ? 'primary' : 'default'">
              {{ scope.row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-switch
              :value="scope.row.status === 'active'"
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
.users-container {
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