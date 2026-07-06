<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>

<template>
  <div class="layout-container">
    <Sidebar />
    <div class="main-content">
      <Header>
        <template #title>
          <slot name="title">RAG文档问答系统</slot>
        </template>
      </Header>
      <main class="content-area">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
  background: #f8fafc;
  overflow: hidden;
}

.main-content {
  flex: 1;
  margin-left: 220px;
  transition: margin-left 0.3s ease;
  overflow: hidden;
}

.sidebar.collapsed + .main-content {
  margin-left: 64px;
}

.content-area {
  padding: 24px;
  min-height: calc(100vh - 64px);
  overflow-x: hidden;
  overflow-y: auto;
}
</style>