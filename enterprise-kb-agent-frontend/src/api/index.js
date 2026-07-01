import axios from 'axios'

// 开发环境通过 Vite 代理 /api -> localhost:8123，避免跨域；生产环境使用相对路径
const API_BASE_URL = '/api'

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 封装SSE连接
export const connectSSE = (url, params, onMessage, onError) => {
  // 构建带参数的URL
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  
  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  
  // 创建EventSource
  const eventSource = new EventSource(fullUrl)
  
  eventSource.onmessage = event => {
    let data = event.data
    
    // 检查是否是特殊标记
    if (data === '[DONE]') {
      if (onMessage) onMessage('[DONE]')
    } else {
      // 处理普通消息
      if (onMessage) onMessage(data)
    }
  }
  
  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }
  
  // 返回eventSource实例，以便后续可以关闭连接
  return eventSource
}

// 企业知识库问答聊天（使用 SseEmitter 端点，与浏览器 EventSource 兼容性更好）
export const chatWithKnowledgeBaseApp = (message, chatId) => {
  return connectSSE('/ai/knowledge_base_app/chat/sse_emitter', { message, chatId })
}

// AI超级智能体聊天
export const chatWithManus = (message) => {
  return connectSSE('/ai/manus/chat', { message })
}

// Generate summary for a selected enterprise document section
export const summarizeDocumentSection = (sectionText, style = 'brief', maxWords = 200) => {
  return request.post('/ai/knowledge_base_app/summary', {
    sectionText,
    style,
    maxWords
  })
}

export default {
  chatWithKnowledgeBaseApp,
  chatWithManus,
  summarizeDocumentSection
}
