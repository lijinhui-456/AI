<template>
  <div class="layout-container">
    <el-container style="height: 100vh">
      <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
        <div class="logo-area" @click="router.push('/dashboard')">
          <span class="logo-icon">⚖️</span>
          <span v-show="!isCollapse" class="logo-text">法务智能工作台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :router="true"
          background-color="#1d1e1f"
          text-color="#bfcbd9"
          active-text-color="#409eff"
          class="sidebar-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon><span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/contract">
            <el-icon><Document /></el-icon><span>合同管理</span>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Collection /></el-icon><span>知识库</span>
          </el-menu-item>
          <el-menu-item index="/chat">
            <el-icon><ChatLineSquare /></el-icon><span>智能对话</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="layout-header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" size="20">
              <Fold v-if="!isCollapse" /><Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown trigger="click">
              <span class="user-info">
                <el-avatar :size="32">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/dashboard')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main class="layout-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/contract')) return '/contract'
  return path
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示').then(() => {
    userStore.logout()
  }).catch(() => {})
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.layout-aside { background-color: #1d1e1f; overflow: hidden; transition: width 0.3s; }
.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-bottom: 1px solid #2d2d2d;
}
.logo-icon { font-size: 28px; margin-right: 8px; }
.logo-text { color: #fff; font-size: 16px; font-weight: 600; white-space: nowrap; }
.sidebar-menu { border-right: none; }
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 60px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { cursor: pointer; color: #606266; }
.header-right { display: flex; align-items: center; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 14px; color: #303133; }
.layout-main { background: #f5f7fa; padding: 20px; overflow-y: auto; }
</style>