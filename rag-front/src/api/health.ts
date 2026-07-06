import { request } from '@/utils/request'

export const checkHealth = () => {
  return request.get<string>('/health', { loading: false })
}