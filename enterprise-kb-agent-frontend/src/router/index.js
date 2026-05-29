import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - NexTech Solutions 企业知识库问答平台',
      description: 'NexTech Solutions 企业知识库问答平台提供企业内部知识库智能问答和 AI 超级智能体服务'
    }
  },
  {
    path: '/knowledge-base',
    name: 'KnowledgeBase',
    component: () => import('../views/KnowledgeBase.vue'),
    meta: {
      title: '企业知识库问答 - NexTech Solutions',
      description: 'NexTech Solutions 企业内部知识库智能问答，解答企业文化、技术规范、办公行政、信息安全等问题'
    }
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: () => import('../views/SuperAgent.vue'),
    meta: {
      title: 'AI超级智能体 - NexTech Solutions',
      description: 'AI 超级智能体可根据需求自主推理和行动，完成复杂任务'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router
