import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, CancelTokenSource } from 'axios'
import { ElMessage } from 'element-plus'

interface ResponseData<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
  success: boolean
}

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  loading?: boolean
  showError?: boolean
}

type RequestKey = string

class Request {
  private instance: AxiosInstance
  private pendingRequests: Map<RequestKey, CancelTokenSource> = new Map()

  constructor() {
    this.instance = axios.create({
      baseURL: '/api',
      timeout: 60000,
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      }
    })

    this.setupInterceptors()
  }

  private getRequestKey(config: InternalAxiosRequestConfig): RequestKey {
    const { method, url, params, data } = config
    return `${method?.toUpperCase() || 'GET'}:${url}:${JSON.stringify(params)}:${JSON.stringify(data)}`
  }

  private addPendingRequest(config: InternalAxiosRequestConfig): void {
    const key = this.getRequestKey(config)
    if (this.pendingRequests.has(key)) {
      const source = this.pendingRequests.get(key)
      source?.cancel('重复请求已被拦截')
    }
    const source = axios.CancelToken.source()
    config.cancelToken = source.token
    this.pendingRequests.set(key, source)
  }

  private removePendingRequest(config: InternalAxiosRequestConfig): void {
    const key = this.getRequestKey(config)
    this.pendingRequests.delete(key)
  }

  private setupInterceptors(): void {
    this.instance.interceptors.request.use(
      (config: CustomAxiosRequestConfig) => {
        this.addPendingRequest(config)
        return config
      },
      (error) => {
        this.handleError(error)
        return Promise.reject(error)
      }
    )

    this.instance.interceptors.response.use(
      (response: AxiosResponse<ResponseData>) => {
        this.removePendingRequest(response.config)
        const { data } = response
        
        if (data.code === 200) {
          return data.data
        } else {
          this.showErrorMessage(data.message)
          return Promise.reject(new Error(data.message))
        }
      },
      (error) => {
        if (!axios.isCancel(error)) {
          this.removePendingRequest(error.config || {})
          this.handleError(error)
        }
        return Promise.reject(error)
      }
    )
  }

  private handleError(error: any): void {
    if (axios.isCancel(error)) {
      return
    }

    let message = '系统异常，请稍后重试'

    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          message = data?.message || '请求参数错误'
          break
        case 401:
          message = '未授权，请登录'
          break
        case 403:
          message = '权限不足'
          break
        case 404:
          message = '资源不存在'
          break
        case 500:
          message = data?.message || '服务器内部错误'
          break
        default:
          message = data?.message || `请求失败，状态码: ${status}`
      }
    } else if (error.request) {
      message = '网络请求超时，请检查网络连接'
    } else {
      message = error.message || message
    }

    this.showErrorMessage(message)
  }

  private showErrorMessage(message: string): void {
    ElMessage({
      type: 'error',
      message,
      duration: 3000
    })
  }

  public get<T = any>(url: string, config?: CustomAxiosRequestConfig): Promise<T> {
    return this.instance.get(url, config)
  }

  public post<T = any>(url: string, data?: any, config?: CustomAxiosRequestConfig): Promise<T> {
    return this.instance.post(url, data, config)
  }

  public put<T = any>(url: string, data?: any, config?: CustomAxiosRequestConfig): Promise<T> {
    return this.instance.put(url, data, config)
  }

  public delete<T = any>(url: string, config?: CustomAxiosRequestConfig): Promise<T> {
    return this.instance.delete(url, config)
  }

  public request<T = any>(config: CustomAxiosRequestConfig): Promise<T> {
    return this.instance.request(config)
  }
}

export const request = new Request()
export type { ResponseData, CustomAxiosRequestConfig }
