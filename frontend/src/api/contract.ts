import request from './request'

export const uploadContract = (formData: FormData): any =>
  request.post('/contract/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

export const getContractPage = (params: any): any =>
  request.get('/contract/page', { params })

export const getContractDetail = (id: number): any =>
  request.get('/contract/' + id)

export const deleteContract = (id: number): any =>
  request.delete('/contract/' + id)

export const getContractRisks = (id: number): any =>
  request.get('/contract/' + id + '/risks')