import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, register, getUserInfo, logout as apiLogout, type LoginData, type RegisterData } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const savedUserInfo = (() => {
    try {
      const s = localStorage.getItem('userInfo')
      return s ? JSON.parse(s) : null
    } catch {
      return null
    }
  })()

  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(savedUserInfo)

  const setToken = (val: string) => {
    token.value = val
    localStorage.setItem('token', val)
  }

  const setUserInfo = (val: any) => {
    userInfo.value = val
    if (val) {
      localStorage.setItem('userInfo', JSON.stringify(val))
    } else {
      localStorage.removeItem('userInfo')
    }
  }

  const loginAction = async (data: LoginData) => {
    const res: any = await login(data)
    if (res.code === 200) {
      setToken(res.data?.token || '')
      const info = res.data?.userInfo || res.data || null
      setUserInfo(info)
      setTimeout(() => {
        window.location.href = '/dashboard'
      }, 100)
    }
    return res
  }

  const registerAction = async (data: RegisterData) => {
    const res: any = await register(data)
    return res
  }

  const fetchUserInfo = async () => {
    try {
      const res: any = await getUserInfo()
      if (res.code === 200) {
        setUserInfo(res.data)
      }
    } catch (e) {
      // ignore
    }
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    apiLogout()
  }

  return {
    token,
    userInfo,
    setToken,
    loginAction,
    registerAction,
    fetchUserInfo,
    logout
  }
})