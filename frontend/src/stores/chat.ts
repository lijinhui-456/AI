import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  type?: string
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const sessionId = ref<string>(Date.now().toString())
  const loading = ref<boolean>(false)

  const addMessage = (role: 'user' | 'assistant' | 'system', content: string, type?: string) => {
    messages.value.push({ role, content, type })
  }

  const updateLastMessage = (content: string) => {
    if (messages.value.length > 0) {
      messages.value[messages.value.length - 1].content += content
    }
  }

  const setLoading = (val: boolean) => {
    loading.value = val
  }

  const removeEmptyAssistantMessage = () => {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant' && !last.content?.trim()) {
      messages.value.pop()
    }
  }

  const clearMessages = () => {
    messages.value = []
    sessionId.value = Date.now().toString()
  }

  const newSession = () => {
    sessionId.value = Date.now().toString()
    messages.value = []
  }

  return {
    messages, sessionId, loading,
    addMessage, updateLastMessage, setLoading,
    removeEmptyAssistantMessage, clearMessages, newSession
  }
})