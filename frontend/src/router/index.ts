import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/login/index.vue') },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
      { path: 'contract', name: 'contract', component: () => import('@/views/contract/index.vue'), meta: { title: '合同管理' } },
      { path: 'contract/:id', name: 'contract-detail', component: () => import('@/views/contract/detail.vue'), meta: { title: '合同详情' } },
      { path: 'chat', name: 'chat', component: () => import('@/views/chat/index.vue'), meta: { title: '智能对话' } },
      { path: 'knowledge', name: 'knowledge', component: () => import('@/views/knowledge/index.vue'), meta: { title: '知识库' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'login' && !token) {
    return { name: 'login' }
  }
  if (to.name === 'login' && token) {
    return { name: 'dashboard' }
  }
})

export default router