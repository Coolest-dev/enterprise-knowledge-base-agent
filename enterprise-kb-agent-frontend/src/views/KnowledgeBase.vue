<template>
  <div class="knowledge-base-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">企业知识库问答</h1>
      <div class="chat-id">会话ID: {{ chatId }}</div>
    </div>
    
    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom 
          :messages="messages" 
          :connection-status="connectionStatus"
          ai-type="knowledge-base"
          @send-message="sendMessage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import { chatWithKnowledgeBaseApp } from '../api'

useHead({
  title: '企业知识库问答 - NexTech Solutions',
  meta: [
    {
      name: 'description',
      content: 'NexTech Solutions 企业内部知识库智能问答，解答企业文化、技术规范、办公行政、信息安全等问题'
    },
    {
      name: 'keywords',
      content: '企业知识库,内部问答,RAG,AI助手,NexTech Solutions,智能客服'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

const generateChatId = () => {
  return 'kb_' + Math.random().toString(36).substring(2, 10)
}

const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

const appendAiContent = (index, text) => {
  const msg = messages.value[index]
  if (!msg) return
  messages.value[index] = { ...msg, content: msg.content + text }
}

const setAiError = (index, text) => {
  const msg = messages.value[index]
  if (!msg) return
  messages.value[index] = { ...msg, content: text }
}

const sendMessage = (message) => {
  addMessage(message, true)
  
  if (eventSource) {
    eventSource.close()
  }
  
  const aiMessageIndex = messages.value.length
  addMessage('', false)
  
  connectionStatus.value = 'connecting'
  eventSource = chatWithKnowledgeBaseApp(message, chatId.value)
  
  eventSource.onopen = () => {
    connectionStatus.value = 'connected'
  }
  
  eventSource.onmessage = (event) => {
    const data = event.data
    if (!data) return
    
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      if (!messages.value[aiMessageIndex]?.content?.trim()) {
        setAiError(aiMessageIndex, '未收到回复内容，请检查 DashScope API Key 是否已在 application-local.yml 中正确配置。')
      }
      eventSource.close()
      return
    }
    
    appendAiContent(aiMessageIndex, data)
  }
  
  eventSource.onerror = () => {
    console.error('SSE Error')
    connectionStatus.value = 'error'
    if (!messages.value[aiMessageIndex]?.content?.trim()) {
      setAiError(
        aiMessageIndex,
        '无法连接后端服务。请确认：\n1. 已在项目目录执行 mvn spring-boot:run（注意是 spring-boot:run）\n2. 后端运行在 http://localhost:8123\n3. application-local.yml 中已填写有效的 spring.ai.dashscope.api-key'
      )
    }
    eventSource.close()
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  chatId.value = generateChatId()
  addMessage('欢迎来到 NexTech Solutions 企业知识库问答系统。您可以咨询企业文化、技术规范、办公行政、信息安全、入职培训、应急响应等内部相关问题。', false)
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.knowledge-base-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f5f8fc;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #1a56db;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: opacity 0.2s;
}

.back-button:hover {
  opacity: 0.8;
}

.back-button:before {
  content: '←';
  margin-right: 8px;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
}

.chat-id {
  font-size: 14px;
  opacity: 0.8;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  min-height: calc(100vh - 56px - 180px);
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }
  
  .title {
    font-size: 18px;
  }
  
  .chat-id {
    font-size: 12px;
  }
  
  .chat-area {
    padding: 12px;
    min-height: calc(100vh - 48px - 160px);
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 10px 12px;
  }
  
  .back-button {
    font-size: 14px;
  }
  
  .title {
    font-size: 16px;
  }
  
  .chat-id {
    display: none;
  }
  
  .chat-area {
    padding: 8px;
    min-height: calc(100vh - 42px - 150px);
    margin-bottom: 8px;
  }
}
</style>
