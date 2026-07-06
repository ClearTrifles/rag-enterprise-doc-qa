<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()

const goBack = () => {
  router.push('/chat')
}
</script>

<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-icon">404</div>
      <h2>页面未找到</h2>
      <p>您访问的页面不存在或已被删除</p>
      <el-button type="primary" icon="ArrowLeft" @click="goBack">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}

.not-found-content {
  text-align: center;
  padding: 60px;
}

.error-icon {
  font-size: 120px;
  font-weight: 900;
  color: #e2e8f0;
  margin-bottom: 24px;
}

h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px 0;
}

p {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 24px 0;
}
</style>