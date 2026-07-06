# RAG文档问答系统 - 前端项目

基于Vue3 + TypeScript + Vite构建的前端应用。

## 技术栈

- Vue 3.4+
- TypeScript 5.4+
- Vite 5.2+
- Element Plus 2.6+
- Pinia 2.1+
- Axios 1.6+

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

启动后访问: http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

### 代码检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

## 项目结构

```
src/
├── api/           # 后端API接口定义
├── components/    # 通用组件
├── hooks/         # 自定义Vue Hooks
├── router/        # 路由配置
├── stores/        # Pinia状态管理
├── utils/         # 工具函数
├── styles/        # 全局样式
├── App.vue        # 根组件
├── main.ts        # 入口文件
└── style.css      # 全局样式文件
```

## 环境变量

### 开发环境 (.env.development)

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=RAG文档问答系统
```

### 生产环境 (.env.production)

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=RAG文档问答系统
```

## 联调说明

前端服务启动后，通过Vite代理自动转发API请求到后端服务 `http://localhost:8080/api/v1`。

## 注意事项

- 确保Node.js版本 >= 18.0.0
- 确保后端服务已启动并运行在端口 8080