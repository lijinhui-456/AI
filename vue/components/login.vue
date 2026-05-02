<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="title">{{ isLogin ? 'AI助手 登录' : 'AI助手 注册' }}</h2>

      <div class="form-item">
        <label>账号</label>
        <input v-model="form.zhanghao" type="text" placeholder="请输入10位数字账号" />
      </div>

      <div class="form-item">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </div>

      <div class="form-item" v-if="!isLogin">
        <label>确认密码</label>
        <input v-model="form.confirmPwd" type="password" placeholder="请确认密码" />
      </div>

      <div class="error-msg" v-if="errorMsg">{{ errorMsg }}</div>

      <button class="submit-btn" @click="submit">
        {{ isLogin ? '登录' : '注册' }}
      </button>

      <div class="switch-text" @click="toggleMode">
        {{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const isLogin = ref(true)
const errorMsg = ref('')

const form = reactive({
  zhanghao: '',
  password: '',
  confirmPwd: ''
})

// 切换模式
const toggleMode = () => {
  isLogin.value = !isLogin.value
  errorMsg.value = ''
  form.confirmPwd = ''
}

// 提交
const submit = async () => {
  errorMsg.value = ''
  const { zhanghao, password, confirmPwd } = form

  if (!zhanghao || !password) {
    errorMsg.value = '账号和密码不能为空！'
    return
  }

  const zhanghaoReg = /^\d{10}$/
  if (!zhanghaoReg.test(zhanghao)) {
    errorMsg.value = '账号必须是 10 位纯数字！'
    return
  }

  if (isLogin.value) {
    await handleLogin()
  } else {
    if (password !== confirmPwd) {
      errorMsg.value = '两次密码输入不一致！'
      return
    }
    await handleRegister()
  }
}

// ====================== 登录 ======================
const handleLogin = async () => {
  try {
    const res = await axios.post('http://localhost:8088/login', {
      zhanghao: form.zhanghao,
      password: form.password
    })

    if (res.data.code === '200') {
      // 存储账号，用于路由守卫判断登录状态
      localStorage.setItem('id', res.data.id)
      localStorage.setItem('zhanghao', form.zhanghao)
      alert('登录成功！')
      // 跳转到聊天页
      router.push('/aichat')
    } else {
      errorMsg.value = '账号或密码错误！'
    }
  } catch (err) {
    console.error('登录失败', err)
    errorMsg.value = '登录失败，请检查后端服务！'
  }
}

// ====================== 注册 ======================
const handleRegister = async () => {
  try {
    const res = await axios.post('http://localhost:8088/zhuce', {
      zhanghao: form.zhanghao,
      password: form.password
    })

    if (res.data.code === '200') {
      alert('注册成功！请登录')
      isLogin.value = true
      form.zhanghao = ''
      form.password = ''
      form.confirmPwd = ''
    } else {
      errorMsg.value = '注册失败！账号可能已存在'
    }
  } catch (err) {
    console.error('注册失败', err)
    errorMsg.value = '注册请求失败，请检查后端服务！'
  }
}
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  background: #f9f9f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  color: #42b983;
  margin-bottom: 30px;
  font-size: 24px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-size: 14px;
}

.form-item input {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  box-sizing: border-box;
}

.form-item input:focus {
  border-color: #42b983;
}

.error-msg {
  color: red;
  font-size: 12px;
  margin-bottom: 15px;
  text-align: center;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  margin-bottom: 20px;
}

.submit-btn:active {
  background: #389f70;
}

.switch-text {
  text-align: center;
  color: #666;
  font-size: 14px;
  cursor: pointer;
}

.switch-text:hover {
  color: #42b983;
}
</style>