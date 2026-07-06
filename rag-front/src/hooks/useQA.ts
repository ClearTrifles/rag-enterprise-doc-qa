import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQAStore } from '@/stores/qa'
import { askQuestionModeC, generalAsk } from '@/api/qa'
import { matchQuickReply } from '@/utils/systemPrompt'
import type { AskRequest, QAAnswer, ChatResponse } from '@/api/types'

export const useQA = () => {
  const qaStore = useQAStore()
  const loading = ref(false)
  const answering = ref(false)

  /**
   * 通用问答（用户确认后调用）
   */
  const handleGeneralAsk = async (question: string, skipAddHistory = false): Promise<ChatResponse> => {
    try {
      const requestData: AskRequest = {
        question,
        sessionId: qaStore.currentSessionId
      }

      const data = await generalAsk(requestData)

      // 转换为兼容格式存入历史
      const answerData: QAAnswer = {
        id: Date.now(),
        question: data.question,
        answer: data.answer,
        sessionId: qaStore.currentSessionId,
        createdAt: new Date().toISOString(),
        replyType: data.replyType,
        referenceList: data.referenceList,
        source: 'general'
      }

      if (!skipAddHistory) {
        qaStore.addHistory(answerData)
        qaStore.setCurrentAnswer(answerData)
      }

      return data
    } catch (error) {
      ElMessage.error('通用问答失败，请稍后重试')
      throw error
    }
  }

  /**
   * 智能问答（模式C折中方案）
   * 检索匹配知识库则基于文档回答；无匹配则返回询问结构，前端弹窗确认后再调用通用大模型问答
   */
  const ask = async (question: string, documentIds?: number[]) => {
    try {
      answering.value = true

      // 首次提问时自动生成会话ID
      if (!qaStore.currentSessionId) {
        qaStore.generateSessionId()
      }

      // 立即添加用户问题到历史记录
      const tempAnswer: QAAnswer = {
        id: Date.now(),
        question,
        answer: '',
        sessionId: qaStore.currentSessionId,
        createdAt: new Date().toISOString(),
        source: 'pending'
      }
      qaStore.addHistory(tempAnswer)

      // 预处理：检查是否是常见问题（如自我介绍、功能介绍等）
      const quickReply = matchQuickReply(question)
      if (quickReply) {
        const data: QAAnswer = {
          id: Date.now(),
          question,
          answer: quickReply,
          sessionId: qaStore.currentSessionId,
          createdAt: new Date().toISOString(),
          source: 'system'
        }
        // 替换之前的临时记录
        qaStore.history.shift()
        qaStore.total.value--
        qaStore.addHistory(data)
        qaStore.setCurrentAnswer(data)
        return data
      }

      const requestData: AskRequest = {
        question,
        sessionId: qaStore.currentSessionId,
        documentIds
      }

      try {
        // 调用模式C问答接口
        const data = await askQuestionModeC(requestData)

        // 分支判断
        switch (data.replyType) {
          case 'RAG_ANSWER':
            // 分支一：检索命中知识库，正常渲染回答和引用溯源
            // 移除临时记录，添加正式回答
            qaStore.history.shift()
            qaStore.total.value--
            handleRAGAnswer(data, question)
            break

          case 'NEED_CONFIRM_GENERAL':
            // 分支二：未检索到知识库内容，更新临时记录为提示信息
            qaStore.updateHistory(tempAnswer.id, {
              answer: '',
              replyType: data.replyType,
              referenceList: data.referenceList,
              source: 'confirm',
              promptTip: data.promptTip
            })
            await handleNeedConfirmGeneral(data, question, tempAnswer.id)
            break

          case 'GENERAL_ANSWER':
            // 分支三：通用自由问答，直接渲染普通回答
            qaStore.history.shift()
            qaStore.total.value--
            handleGeneralAnswer(data, question)
            break

          default:
            ElMessage.warning('未知的应答类型')
            break
        }

        return data
      } catch (error) {
        // 接口调用失败，更新临时记录的回答为错误提示
        qaStore.updateHistory(tempAnswer.id, {
          answer: '抱歉，系统暂时无法回答您的问题，请稍后再试。',
          source: 'error'
        })
        ElMessage.error('问答失败，请稍后重试')
        throw error
      }
    } finally {
      answering.value = false
    }
  }

  /**
   * 分支一：RAG_ANSWER - 检索命中知识库，正常渲染回答和引用溯源
   */
  const handleRAGAnswer = (data: ChatResponse, question: string) => {
    const answerData: QAAnswer = {
      id: Date.now(),
      question: data.question,
      answer: data.answer,
      sessionId: qaStore.currentSessionId,
      createdAt: new Date().toISOString(),
      replyType: data.replyType,
      referenceList: data.referenceList,
      source: 'rag'
    }

    qaStore.addHistory(answerData)
    qaStore.setCurrentAnswer(answerData)
  }

  /**
   * 分支二：NEED_CONFIRM_GENERAL - 未检索到知识库，弹出确认框询问用户
   */
  const handleNeedConfirmGeneral = async (data: ChatResponse, question: string, tempAnswerId?: number): Promise<void> => {
    try {
      // 弹出确认框
      await ElMessageBox.confirm(data.promptTip, '知识库无匹配结果', {
        confirmButtonText: '开启通用问答',
        cancelButtonText: '取消',
        type: 'info',
        center: true
      })

      // 用户点击确定，调用通用问答接口（跳过添加历史，由调用方处理）
      const generalData = await handleGeneralAsk(question, true)

      // 如果有临时记录ID，替换它而不是添加新记录
      if (tempAnswerId) {
        qaStore.updateHistory(tempAnswerId, {
          answer: generalData.answer,
          replyType: generalData.replyType,
          referenceList: generalData.referenceList,
          source: 'general'
        })
      }
    } catch {
      // 用户点击取消或关闭弹窗，不做任何操作
      console.log('用户取消通用问答')
    }
  }

  /**
   * 分支三：GENERAL_ANSWER - 通用自由问答，直接渲染普通回答
   */
  const handleGeneralAnswer = (data: ChatResponse, question: string) => {
    const answerData: QAAnswer = {
      id: Date.now(),
      question: data.question,
      answer: data.answer,
      sessionId: qaStore.currentSessionId,
      createdAt: new Date().toISOString(),
      replyType: data.replyType,
      referenceList: data.referenceList,
      source: 'general'
    }

    qaStore.addHistory(answerData)
    qaStore.setCurrentAnswer(answerData)
  }

  const fetchHistory = async (params?: any) => {
    try {
      loading.value = true
      // TODO: 实现历史记录获取
      return null
    } finally {
      loading.value = false
    }
  }

  const startNewSession = () => {
    qaStore.generateSessionId()
    qaStore.setCurrentAnswer(null)
  }

  const clearAllHistory = () => {
    qaStore.clearHistory()
  }

  return {
    qaStore,
    loading,
    answering,
    ask,
    fetchHistory,
    startNewSession,
    clearAllHistory
  }
}
