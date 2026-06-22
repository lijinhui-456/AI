import request from './request'

export interface LoginData {
  username: string
  password: string
}

export interface RegisterData {
  username: string
  password: string
  nickname: string
  email: string
}

export const login = (data: LoginData): any =>
  request.post('/auth/login', data)

export const register = (data: RegisterData): any =>
  request.post('/auth/register', data)

export const getUserInfo = (): any =>
  request.get('/auth/info')

export const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  window.location.href = '/login'
}