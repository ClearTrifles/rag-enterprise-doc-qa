<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Eye, EyeOff } from '@element-plus/icons-vue'

const router = useRouter()
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const isLoading = ref(false)

const handleLogin = async () => {
  if (!username.value || !password.value) {
    alert('请输入用户名和密码')
    return
  }
  
  isLoading.value = true
  await new Promise(resolve => setTimeout(resolve, 1000))
  isLoading.value = false
  
  router.push('/chat')
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="logo-section">
        <div class="logo">
          <User class="logo-icon" />
        </div>
        <h1>RAG文档问答系统</h1>
        <p>企业级智能问答平台</p>
      </div>
      
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-item">
          <label>用户名</label>
          <el-input
            v-model="username"
            placeholder="请输入用户名"
            class="form-input"
          >
            <template #prefix>
              <User />
            </template>
          </el-input>
        </div>
        
        <div class="form-item">
          <label>密码</label>
          <el-input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            class="form-input"
          >
            <template #prefix>
              <Lock />
            </template>
            <template #suffix>
              <el-icon @click="showPassword = !showPassword">
                <Eye v-if="showPassword" />
                <EyeOff v-else />
              </el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="form-item">
          <el-checkbox>记住我</el-checkbox>
          <span class="forgot-password">忘记密码？</span>
        </div>
        
        <el-button type="primary" class="login-btn" :loading="isLoading" @click="handleLogin">
          登录
        </el-button>
      </form>
      
      <div class="register-link">
        还没有账号？<span>立即注册</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}

.login-box {
  width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.logo-section {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  color: #fff;
}

.logo-section h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.logo-section p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.login-form {
  margin-bottom: 24px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
}

.form-item:last-of-type {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-password {
  font-size: 14px;
  color: #3b82f6;
  cursor: pointer;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.register-link {
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

.register-link span {
  color: #3b82f6;
  cursor: pointer;
}
</style>