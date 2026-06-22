import request from './request'

export interface ChatRequest {
  sessionId: string
  content: string
  contractId?: number
}

export const aiChat = (data: ChatRequest): any =>
  request.post('/ai/chat', data)

export const aiChatStream = (
  data: ChatRequest,
  onMessage: (event: string, data: string) => void,
  timeoutMs = 30000
) => {
  const token = localStorage.getItem('token') || ''
  const controller = new AbortController()
  const timeoutId = setTimeout(() => {
    onMessage('error', '请求超时，请检查后端服务是否正常运行')
    controller.abort()
  }, timeoutMs)

  return fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token,
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache'
    },
    body: JSON.stringify(data),
    signal: controller.signal
  })
    .then(async (response) => {
      clearTimeout(timeoutId)
      if (!response.ok) {
        const errText = await response.text().catch(() => '')
        throw new Error('HTTP ' + response.status + (errText ? ': ' + errText : ''))
      }

      const reader = response.body?.getReader()
      if (!reader) throw new Error('无法读取响应流')

      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let receivedAny = false

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        let idx = buffer.indexOf('\n\n')
        while (idx !== -1) {
          const block = buffer.substring(0, idx)
          buffer = buffer.substring(idx + 2)
          idx = buffer.indexOf('\n\n')
          if (!block.trim()) continue

          let eventName = ''
          let eventData = ''
          const lines = block.split('\n')
          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventName = line.substring(6).trim()
            } else if (line.startsWith('data:')) {
              if (eventData) eventData += '\n'
              eventData += line.substring(5).trimStart()
            }
          }
          if (eventName && eventData !== '') {
            receivedAny = true
            onMessage(eventName, eventData)
          }
        }
      }

      if (!receivedAny) {
        onMessage('content', '（已完成对话）')
      }
      onMessage('end', 'done')
    })
    .catch((err) => {
      clearTimeout(timeoutId)
      if (err.name !== 'AbortError' || !controller.signal.aborted) {
        onMessage('error', err.message || '对话失败')
      }
    })
}

export const generateContract = (data: any): any =>
  request.post('/ai/generate-contract', data)

export const analyzeContract = (contractId: number): any =>
  request.post('/ai/analyze/' + contractId)

export const getChatHistory = (sessionId: string): any =>
  request.get('/ai/history/' + sessionId)