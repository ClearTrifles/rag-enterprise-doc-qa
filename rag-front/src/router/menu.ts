import type { RouteRecordRaw } from 'vue-router'
import {
  MessageSquare,
  FolderOpen,
  History,
  Settings,
  Login,
  Users,
  FileText,
  Tags,
  Sliders
} from '@element-plus/icons-vue'

export interface MenuItem {
  path: string
  name: string
  icon: typeof MessageSquare
  show: boolean
  meta?: Record<string, unknown>
}

export const menuItems: MenuItem[] = [
  {
    path: '/chat',
    name: '智能问答',
    icon: MessageSquare,
    show: true,
    meta: { title: '智能问答对话页' }
  },
  {
    path: '/documents',
    name: '知识库管理',
    icon: FolderOpen,
    show: true,
    meta: { title: '文档上传与管理' }
  },
  {
    path: '/history',
    name: '问答记录',
    icon: History,
    show: true,
    meta: { title: '历史问答记录查询' }
  },
  {
    path: '/settings',
    name: '个人设置',
    icon: Settings,
    show: true,
    meta: { title: '模型参数配置' }
  },
  {
    path: '/login',
    name: '用户登录',
    icon: Login,
    show: false,
    meta: { title: '用户登录页面' }
  },
  {
    path: '/users',
    name: '权限管理',
    icon: Users,
    show: false,
    meta: { title: '用户权限管理' }
  },
  {
    path: '/logs',
    name: '操作日志',
    icon: FileText,
    show: false,
    meta: { title: '系统操作日志' }
  },
  {
    path: '/categories',
    name: '分类管理',
    icon: Tags,
    show: false,
    meta: { title: '知识库分类管理' }
  },
  {
    path: '/system',
    name: '系统配置',
    icon: Sliders,
    show: false,
    meta: { title: '系统参数配置中心' }
  }
]

export const currentMenuItems = menuItems.filter(item => item.show)
export const reservedMenuItems = menuItems.filter(item => !item.show)

export const routeConfig: RouteRecordRaw[] = menuItems.map(item => ({
  path: item.path,
  name: item.name.replace(/\s/g, ''),
  meta: {
    title: item.name,
    showMenu: item.show,
    ...item.meta
  }
}))