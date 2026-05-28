import { createRouter, createWebHistory } from 'vue-router'
import chat from '../components/aichat.vue'
import login from '../components/login.vue'

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        component: login,
    },
    {
        path: '/aichat',
        component: chat,
        meta: { requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：必须登录才能进 aichat
router.beforeEach((to, from, next) => {
    // 你登录存在 local 的是 zhanghao，所以用这个判断
    const isLogin = localStorage.getItem('zhanghao')

    // 需要权限，但没登录 → 去 login
    if (to.meta.requiresAuth && !isLogin) {
        next('/login')
    }
    // 其他情况都放行
    else {
        next()
    }
})

export default router