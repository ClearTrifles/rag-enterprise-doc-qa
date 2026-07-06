import { request } from '@/utils/request'
import type { QAAnswer, PageResult, AskRequest, QAHistoryParams, ChatResponse } from './types'

export const askQuestion = (data: AskRequest) => {
  return request.post<QAAnswer>('/qa/ask', data, { loading: true })
}

/**
 * 智能问答（模式C折中方案）
 * 检索匹配知识库则基于文档回答；无匹配则返回询问结构，前端弹窗确认后再调用通用大模型问答
 */
export const askQuestionModeC = (data: AskRequest) => {
  return request.post<ChatResponse>('/qa/ask/mode-c', data, { loading: true })
}

/**
 * 通用自由问答（无知识库限制）
 * 适用于闲聊、常识、外部资讯类问题
 */
export const generalAsk = (data: AskRequest) => {
  return request.post<ChatResponse>('/qa/general-ask', data, { loading: true })
}

export const getQAHistory = (params: QAHistoryParams = {}) => {
  return request.get<PageResult<QAAnswer>>('/qa/history', { params, loading: true })
}