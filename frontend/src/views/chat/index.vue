<template>
  <div class="chat-wrapper">
    <div class="chat-header">
      <div class="chat-title">
        <el-icon :size="20"><ChatDotRound /></el-icon>
        <span>智能对话</span>
      </div>
      <div class="chat-actions">
        <el-button size="small" @click="handleNewSession">
          <el-icon><Refresh /></el-icon>
          新会话
        </el-button>
      </div>
    </div>

    <div ref="messageContainer" class="chat-messages">
      <div v-if="messages.length === 0" class="chat-empty">
        <el-empty description="开始与 AI 对话，或发送合同内容进行智能分析" />
        <div class="quick-actions">
          <el-tag
            v-for="q in quickQuestions"
            :key="q"
            type="info"
            size="large"
            class="quick-tag"
            @click="sendQuick(q)"
            effect="plain"
          >
            {{ q }}
          </el-tag>
        </div>
      </div>

      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="msg-row"
        :class="msg.role"
      >
        <div class="msg-avatar">
          <el-avatar :size="36" :style="msg.role === 'user' ? 'background:#409eff' : 'background:#67c23a'">
            {{ msg.role === 'user' ? '我' : 'AI' }}
          </el-avatar>
        </div>
        <div class="msg-bubble">
          <div v-if="msg.type === 'tool'" class="msg-tool">
            <el-icon><Tools /></el-icon>
            <span>{{ msg.content }}</span>
          </div>
          <div v-else-if="msg.role === 'assistant'" class="msg-content ai-content">
            <div v-html="renderMarkdown(msg.content)"></div>
            <span v-if="loading && idx === messages.length - 1" class="cursor"></span>
          </div>
          <div v-else class="msg-content">
            {{ msg.content }}
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        :disabled="loading"
        resize="none"
        @keydown="handleKeyDown"
      />
      <el-button type="primary" :loading="loading" class="send-btn" @click="sendMessage">
        {{ loading ? '思考中...' : '发送' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Refresh, Tools } from '@element-plus/icons-vue'
import { aiChatStream } from '@/api/ai'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const messages = computed(() => chatStore.messages)
const loading = computed(() => chatStore.loading)

const inputText = ref('')
const messageContainer = ref<HTMLElement | null>(null)
let safetyTimer: number | null = null
let streamActive = false

const quickQuestions = [
  '帮我分析一下劳动合同的风险点',
  '什么是竞业限制条款？',
  '如何起草一份采购合同？',
  '解释一下违约金和赔偿金的区别'
]

const sendQuick = (q: string) => {
  inputText.value = q
  sendMessage()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

watch(messages, () => scrollToBottom(), { deep: true })

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const stopLoading = () => {
  chatStore.setLoading(false)
  streamActive = false
  if (safetyTimer !== null) {
    clearTimeout(safetyTimer)
    safetyTimer = null
  }
  chatStore.removeEmptyAssistantMessage()
}

const handleNewSession = () => {
  chatStore.newSession()
  ElMessage.success('已开启新会话')
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) return
  if (loading.value) {
    ElMessage.warning('AI 正在思考，请稍候...')
    return
  }

  inputText.value = ''
  chatStore.addMessage('user', text)
  chatStore.addMessage('assistant', '')
  chatStore.setLoading(true)
  streamActive = true
  scrollToBottom()

  safetyTimer = window.setTimeout(() => {
    if (streamActive) {
      const last = chatStore.messages[chatStore.messages.length - 1]
      if (!last?.content?.trim()) {
        chatStore.updateLastMessage('（AI 响应超时，请检查后端服务或重试）')
      }
      stopLoading()
      scrollToBottom()
    }
  }, 30000)

  try {
    await aiChatStream(
      { sessionId: chatStore.sessionId, content: text },
      (event: string, data: string) => {
        if (event === 'content' || event === 'stream') {
          chatStore.updateLastMessage(data)
          scrollToBottom()
        } else if (event === 'tool') {
          chatStore.addMessage('assistant', data, 'tool')
          chatStore.addMessage('assistant', '')
          scrollToBottom()
        } else if (event === 'error') {
          const last = chatStore.messages[chatStore.messages.length - 1]
          if (!last?.content?.trim()) {
            chatStore.updateLastMessage('⚠️ ' + data)
          } else {
            chatStore.addMessage('assistant', '⚠️ ' + data)
          }
          stopLoading()
          scrollToBottom()
        } else if (event === 'end') {
          stopLoading()
          scrollToBottom()
        }
      }
    )
    setTimeout(() => {
      if (chatStore.loading) {
        stopLoading()
      }
    }, 500)
  } catch (e: any) {
    chatStore.updateLastMessage('对话失败: ' + (e.message || '未知错误'))
    stopLoading()
  }
}

const renderMarkdown = (text: string): string => {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br/>')
  return html
}

onMounted(() => scrollToBottom())
onBeforeUnmount(() => {
  if (safetyTimer !== null) clearTimeout(safetyTimer)
})
</script>

<style scoped>
.chat-wrapper {
  height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}
.chat-header {
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.chat-actions { display: flex; gap: 10px; align-items: center; }
.chat-messages { flex: 1; overflow-y: auto; padding: 24px; }
.chat-empty { text-align: center; padding: 40px 20px; }
.quick-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}
.quick-tag { cursor: pointer; padding: 8px 14px; }
.msg-row {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  gap: 12px;
}
.msg-row.user { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.msg-bubble { max-width: 70%; }
.msg-content {
  background: #fff;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.7;
  color: #303133;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.msg-row.user .msg-content { background: #409eff; color: #fff; }
.msg-row.user .msg-content :deep(code) { background: rgba(255,255,255,0.2); color: #fff; }
.ai-content { min-height: 32px; }
.ai-content :deep(code) { background: #f0f2f5; padding: 2px 6px; border-radius: 4px; font-family: 'SF Mono', Consolas, monospace; font-size: 13px; }
.ai-content :deep(strong) { color: #e6a23c; }
.msg-tool {
  background: #ecf5ff;
  color: #409eff;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.cursor {
  display: inline-block;
  width: 8px;
  height: 18px;
  background: #409eff;
  margin-left: 2px;
  vertical-align: middle;
  animation: blink 0.9s steps(1) infinite;
}
@keyframes blink {
  0%, 49% { opacity: 1; }
  50%, 100% { opacity: 0; }
}
.chat-input {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
.send-btn { height: 56px; min-width: 90px; }
</style>