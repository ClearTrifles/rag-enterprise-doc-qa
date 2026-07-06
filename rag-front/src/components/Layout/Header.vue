<script setup lang="ts">
</script>

<template>
  <header class="header">
    <div class="header-left">
      <slot name="title"></slot>
    </div>
    
    <div class="header-right">
      <div class="user-area">
        <div class="user-btn">
          <div class="user-avatar">U</div>
          <span class="user-name">管理员</span>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 90;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-left {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-area {
  position: relative;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px 6px 8px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #475569;
}

.user-btn:hover {
  background: #f8fafc;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}
</style>
