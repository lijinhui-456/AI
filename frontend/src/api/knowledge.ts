import request from './request'

export const getKnowledgePage = (params: any): any =>
  request.get('/knowledge/page', { params })

export const getKnowledgeDetail = (id: number): any =>
  request.get('/knowledge/' + id)

export const createKnowledge = (data: any): any =>
  request.post('/knowledge', data)

export const updateKnowledge = (data: any): any =>
  request.put('/knowledge', data)

export const deleteKnowledge = (id: number): any =>
  request.delete('/knowledge/' + id)

export const searchKnowledge = (params: any): any =>
  request.get('/knowledge/search', { params })