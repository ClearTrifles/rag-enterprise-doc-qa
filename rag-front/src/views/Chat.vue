<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useQA } from '@/hooks/useQA';
import { uploadDocument } from '@/api/document';

const { qaStore, answering, ask, startNewSession } = useQA();
const inputMessage = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const uploading = ref(false);

interface MessageItem {
  id: number;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  replyType?: string;
  referenceList?: Array<{
    documentId: number;
    documentName?: string;
    content: string;
    score?: number;
  }>;
  isLoading?: boolean;
}

const displayMessages = computed(() => {
  const messages: MessageItem[] = [];

  for (let i = qaStore.history.length - 1; i >= 0; i--) {
    const item = qaStore.history[i];
    messages.push({
      id: i * 2,
      type: 'user' as const,
      content: item.question || '',
      timestamp: new Date(item.createdAt || Date.now())
    });
    messages.push({
      id: i * 2 + 1,
      type: 'assistant' as const,
      content: item.answer || item.promptTip || '',
      timestamp: new Date(item.createdAt || Date.now()),
      replyType: item.replyType,
      referenceList: item.referenceList,
      isLoading: item.source === 'pending' && !item.answer
    });
  }

  return messages;
});

const hasSession = computed(() => !!qaStore.currentSessionId);

const scrollToBottom = async () => {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

const handleSend = async () => {
  if (!inputMessage.value.trim()) return;

  const question = inputMessage.value.trim();
  inputMessage.value = '';
  
  await scrollToBottom();

  try {
    await ask(question);
    await scrollToBottom();
  } catch (error) {
    ElMessage.error('问答失败，请稍后重试');
    await scrollToBottom();
  }
};

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    handleSend();
  }
};

const handleNewSession = () => {
  startNewSession();
};

const handleFileUpload = () => {
  fileInput.value?.click();
};

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    const file = target.files[0];
    
    // 验证文件类型
    const allowedTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/msword', 'text/plain', 'text/markdown'];
    if (!allowedTypes.includes(file.type) && !file.name.match(/\.(pdf|docx|doc|txt|md)$/i)) {
      ElMessage.error('请上传PDF、DOC、DOCX、TXT或MD格式的文件');
      target.value = '';
      return;
    }

    // 验证文件大小（最大100MB）
    if (file.size > 100 * 1024 * 1024) {
      ElMessage.error('文件大小不能超过100MB');
      target.value = '';
      return;
    }

    uploading.value = true;
    try {
      const documentName = file.name.replace(/\.[^/.]+$/, '');
      await uploadDocument(file, documentName);
      ElMessage.success(`文件 "${file.name}" 上传成功，正在处理中...`);
    } catch (error) {
      ElMessage.error('文件上传失败，请稍后重试');
    } finally {
      uploading.value = false;
      target.value = '';
    }
  }
};

const getReplyTypeLabel = (replyType?: string) => {
  switch (replyType) {
    case 'RAG_ANSWER':
      return '知识库检索回答';
    case 'GENERAL_ANSWER':
      return '通用问答';
    default:
      return '';
  }
};

onMounted(() => {
  scrollToBottom();
});
</script>

<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="header-left">
        <h2>智能问答</h2>
        <span v-if="hasSession" class="session-info">
          会话 ID: {{ qaStore.currentSessionId?.slice(-8) }}
        </span>
        <span v-else class="session-info">
          开始你的第一个对话吧
        </span>
      </div>
      <div class="header-actions">
        <el-button size="small" @click="handleFileUpload" title="上传文件">
          📎 上传
        </el-button>
        <el-button size="small" @click="handleNewSession" title="新建会话">
          🔄 新建会话
        </el-button>
      </div>
    </div>
    
    <div ref="messagesContainer" class="messages-container">
      <div v-if="displayMessages.length === 0" class="welcome-message">
        <div class="welcome-icon">🤖</div>
        <h3>欢迎使用智能问答</h3>
        <p>我是您的智能助手，可以帮助您解答问题。</p>
        <p>请输入您的问题，我将为您提供专业的回答。</p>
      </div>
      
      <div 
        v-for="msg in displayMessages" 
        :key="msg.id" 
        class="message-item"
        :class="msg.type"
      >
        <div class="message-avatar">
          {{ msg.type === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="message-content">
          <div v-if="msg.isLoading" class="typing-indicator">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
          <div v-else class="message-text">{{ msg.content }}</div>
          <div class="message-time">
            {{ msg.timestamp.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}
          </div>
          
          <!-- RAG_ANSWER 引用溯源列表 -->
          <div v-if="msg.type === 'assistant' && msg.replyType === 'RAG_ANSWER' && msg.referenceList && msg.referenceList.length > 0" class="reference-section">
            <div class="reference-header">
              <span class="reference-label">📚 引用溯源</span>
              <span class="reference-count">{{ msg.referenceList.length }} 条相关文档</span>
            </div>
            <div class="reference-list">
              <div 
                v-for="(ref, index) in msg.referenceList" 
                :key="index" 
                class="reference-item"
              >
                <div class="reference-doc-info">
                  <span class="reference-doc-name">{{ ref.documentName || '未知文档' }}</span>
                  <span v-if="ref.score" class="reference-score">匹配度: {{ (ref.score * 100).toFixed(1) }}%</span>
                </div>
                <div class="reference-content">{{ ref.content }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="input-area">
      <div class="input-wrapper">
        <textarea
          v-model="inputMessage"
          placeholder="请输入您的问题..."
          class="message-input"
          @keydown="handleKeyDown"
          :disabled="answering"
        />
      </div>
      <div class="input-actions">
        <input
          ref="fileInput"
          type="file"
          class="hidden-file-input"
          accept=".pdf,.docx,.md,.txt"
          @change="handleFileChange"
        />
        <el-button size="small" @click="handleFileUpload" :disabled="answering || uploading" :loading="uploading">
          📎 上传
        </el-button>
        <el-button 
          type="primary" 
          @click="handleSend"
          :disabled="!inputMessage.trim() || answering"
          :loading="answering"
        >
          {{ answering ? '思考中...' : '发送' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-left h2 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.session-info {
  font-size: 12px;
  color: #94a3b8;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.messages-container {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.welcome-message h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.welcome-message p {
  font-size: 14px;
  color: #64748b;
  margin: 4px 0;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: messageFadeIn 0.2s ease;
}

.message-item.user {
  flex-direction: row-reverse;
}

@keyframes messageFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.message-item.user .message-avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.message-item.assistant .message-avatar {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 75%;
}

.message-item.user .message-content {
  align-items: flex-end;
}

.message-item.assistant .message-content {
  align-items: flex-start;
}

.message-text {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-item.user .message-text {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #fff;
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .message-text {
  background: #f1f5f9;
  color: #1e293b;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 16px;
}

.message-time {
  font-size: 11px;
  color: #94a3b8;
}

/* 引用溯源样式 */
.reference-section {
  margin-top: 8px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.reference-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.reference-label {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.reference-count {
  font-size: 11px;
  color: #64748b;
}

.reference-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reference-item {
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.reference-doc-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.reference-doc-name {
  font-size: 12px;
  font-weight: 500;
  color: #3b82f6;
}

.reference-score {
  font-size: 11px;
  color: #10b981;
}

.reference-content {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f1f5f9;
  border-radius: 16px;
  border-bottom-left-radius: 4px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border-radius: 50%;
  animation: typingBounce 1.4s infinite ease-in-out;
}

.typing-dot:nth-child(1) { animation-delay: 0s; }
.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}

.input-wrapper {
  margin-bottom: 12px;
}

.message-input {
  width: 100%;
  min-height: 80px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.message-input:focus {
  border-color: #3b82f6;
}

.message-input::placeholder {
  color: #94a3b8;
}

.message-input:disabled {
  background: #f8fafc;
  cursor: not-allowed;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.hidden-file-input {
  display: none;
}
</style>
