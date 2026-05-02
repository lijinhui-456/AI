<template>
  <div class="chat-container">
    <div class="chat-header">
      AI 图文对话助手
      <button @click="logout" class="logout-btn">退出登录</button>
    </div>

    <div class="chat-messages" ref="msgContainer">
      <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', msg.role === 'user' ? 'user-msg' : 'ai-msg']"
      >
        <div v-if="msg.role === 'user'">
          <p class="msg-content">{{ msg.content }}</p>
          <img v-if="msg.imageUrl" :src="msg.imageUrl" class="msg-image" />
        </div>
        <div v-else class="msg-content">{{ msg.content }}</div>
      </div>

      <div v-if="isLoading" class="loading">AI思考中...</div>
    </div>

    <div class="chat-input">
      <label class="image-upload">
        <input
            type="file"
            accept="image/*"
            @change="handleFileChange"
            :disabled="isLoading"
            hidden
        />
        <span>上传图片</span>
      </label>

      <div v-if="previewImage" class="preview-box">
        <img :src="previewImage" alt="预览" />
        <button @click="clearImage">✕</button>
      </div>

      <input
          v-model="inputText"
          type="text"
          placeholder="请输入问题..."
          @keyup.enter="sendMessage"
          :disabled="isLoading"
      />

      <button
          @click="sendMessage"
          :disabled="isLoading || (!inputText && !selectedFile)"
          class="send-btn"
      >
        发送
      </button>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const userId = ref('')

onMounted(() => {
  const id = localStorage.getItem('id')
  if (!id) {
    alert('请先登录')
    router.push('/login')
    return
  }
  userId.value = id
})

const messages = ref([])
const inputText = ref('')
const selectedFile = ref(null)
const previewImage = ref('')
const isLoading = ref(false)
const msgContainer = ref(null)

// 退出登录
const logout = () => {
  localStorage.removeItem('id')
  localStorage.removeItem('zhanghao')
  alert('退出成功')
  router.push('/login')
}

// 滚动到底
const scrollToBottom = async () => {
  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

// 图片压缩到 360px
const compressImage = (file) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.src = URL.createObjectURL(file)
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      let w = img.width, h = img.height
      const max = 360

      if (w > h && w > max) { h = h * max / w; w = max }
      else if (h > max) { w = w * max / h; h = max }

      canvas.width = w
      canvas.height = h
      ctx.drawImage(img, 0, 0, w, h)

      canvas.toBlob((blob) => {
        resolve(blob)
      }, 'image/jpeg', 0.7)
    }
  })
}

// 选择图片（已加压缩）
const handleFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  const compressedBlob = await compressImage(file)
  const compressedFile = new File([compressedBlob], 'compressed.jpg', { type: 'image/jpeg' })

  selectedFile.value = compressedFile
  previewImage.value = URL.createObjectURL(compressedBlob)
}

// 清除图片
const clearImage = () => {
  selectedFile.value = null
  previewImage.value = ''
}


const streamSSE = async (url, options) => {
  isLoading.value = true
  const aiIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '' })
  scrollToBottom()

  try {
    const res = await fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        Accept: 'text/event-stream',
      },
    })

    if (!res.ok) throw new Error('请求失败')

    const reader = res.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value)
      const lines = text.split('\n').filter(line => line.trim().startsWith('data:'))

      for (let line of lines) {
        let content = line.replace('data:', '').trim()
        if (content === '[DONE]') continue
        messages.value[aiIndex].content += content
        scrollToBottom()
      }
    }
  } catch (err) {
    messages.value[aiIndex].content = '连接失败，请重试'
    console.error(err)
  } finally {
    isLoading.value = false
  }
}
// 发送文本
const sendTextMessage = () => {
  messages.value.push({
    role: 'user',
    content: inputText.value,
    imageUrl: '',
  })
  scrollToBottom()

  streamSSE('http://localhost:8088/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      id: userId.value,
      message: inputText.value,
    }),
  })

  inputText.value = ''
}
// 发送图片
const sendImageMessage = async () => {
  messages.value.push({
    role: 'user',
    content: inputText.value || '描述图片',
    imageUrl: previewImage.value,
  })
  scrollToBottom()

  const formData = new FormData()
  formData.append('id', userId.value)
  formData.append('image', selectedFile.value)
  formData.append('message', inputText.value || '请描述这张图片')

  await streamSSE('http://localhost:8088/ai/chat-with-image', {
    method: 'POST',
    body: formData,
  })

  inputText.value = ''
  clearImage()
}

const sendMessage = () => {
  if (isLoading.value) return
  if (selectedFile.value) {
    sendImageMessage()
  } else if (inputText.value.trim()) {
    sendTextMessage()
  }
}
</script>

<style scoped>
.chat-container {
  max-width: 800px;
  margin: 20px auto;
  height: 90vh;
  display: flex;
  flex-direction: column;
  border: 1px solid #eee;
  border-radius: 10px;
  overflow: hidden;
  background: #f9f9f9;
}

.chat-header {
  padding: 15px;
  background: #42b983;
  color: white;
  font-size: 18px;
  text-align: center;
  font-weight: bold;
  position: relative;
}

.logout-btn {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  background: #ff4444;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-item {
  max-width: 70%;
  display: flex;
}

.user-msg {
  margin-left: auto;
  flex-direction: row-reverse;
}

.ai-msg {
  margin-right: auto;
}

.msg-content {
  padding: 10px 15px;
  border-radius: 15px;
  line-height: 1.5;
  margin: 0;
  word-break: break-all;
}

.user-msg .msg-content {
  background: #42b983;
  color: white;
  border-bottom-right-radius: 0;
}

.ai-msg .msg-content {
  background: white;
  color: #333;
  border: 1px solid #eee;
  border-bottom-left-radius: 0;
}

.msg-image {
  max-width: 150px;
  border-radius: 10px;
  margin-top: 5px;
}

.loading {
  text-align: center;
  color: #666;
  padding: 10px;
}

.chat-input {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  background: white;
  border-top: 1px solid #eee;
}

.image-upload {
  cursor: pointer;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  white-space: nowrap;
}

.preview-box {
  position: relative;
  width: 40px;
  height: 40px;
}

.preview-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 5px;
}

.preview-box button {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: red;
  color: white;
  border: none;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.chat-input input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
}

.send-btn {
  padding: 10px 20px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>