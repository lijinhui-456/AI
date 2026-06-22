<template>
  <div class="contract-page">
    <el-card class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-input v-model="query.keyword" placeholder="搜索合同名称" clearable @clear="search" @keyup.enter="search" />
        </el-col>
        <el-col :span="4">
          <el-select v-model="query.status" placeholder="状态" clearable @change="search" style="width:100%">
            <el-option label="待审核" value="pending" />
            <el-option label="审核中" value="reviewing" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="query.type" placeholder="类型" clearable @change="search" style="width:100%">
            <el-option label="采购合同" value="purchase" />
            <el-option label="销售合同" value="sale" />
            <el-option label="保密协议" value="NDA" />
            <el-option label="劳动合同" value="employment" />
            <el-option label="服务合同" value="service" />
            <el-option label="租赁合同" value="lease" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-col>
        <el-col :span="6" :offset="4" style="text-align:right">
          <el-button type="primary" @click="showUpload = true">
            <el-icon><Upload /></el-icon> 上传合同
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="showUpload" title="上传合同" width="500px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="合同名称">
          <el-input v-model="uploadForm.title" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="uploadForm.type" placeholder="选择类型" style="width:100%">
            <el-option label="采购合同" value="purchase" />
            <el-option label="销售合同" value="sale" />
            <el-option label="保密协议" value="NDA" />
            <el-option label="劳动合同" value="employment" />
            <el-option label="服务合同" value="service" />
            <el-option label="租赁合同" value="lease" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.doc,.docx,.txt"
            :on-change="handleFileChange"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 pdf, doc, docx, txt 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">确认上传</el-button>
      </template>
    </el-dialog>

    <el-card>
      <el-table :data="contracts" stripe v-loading="loading" style="width:100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="title" label="合同名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" :type="riskTag(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="风险评分" width="90">
          <template #default="{ row }">
            <span v-if="row.riskScore != null">{{ row.riskScore }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="router.push('/contract/' + row.id)">查看</el-button>
            <el-button size="small" type="warning" link @click="handleAnalyze(row)" :disabled="row.status==='reviewing'">AI审核</el-button>
            <el-popconfirm title="确定删除该合同？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="search"
          @current-change="search"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getContractPage, uploadContract, deleteContract } from '@/api/contract'
import { analyzeContract } from '@/api/ai'
import type { UploadFile } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const uploading = ref(false)
const contracts = ref<any[]>([])
const total = ref(0)
const showUpload = ref(false)
const selectedFile = ref<File | null>(null)
const query = reactive({ page: 1, size: 10, keyword: '', status: '', type: '' })
const uploadForm = reactive({ title: '', type: 'other' })

const typeLabel = (t: string) => ({ purchase: '采购', sale: '销售', NDA: '保密协议', employment: '劳动', service: '服务', lease: '租赁', other: '其他' }[t] || t)
const typeTag = (t: string) => ({ purchase: 'primary', sale: 'primary', NDA: 'warning', employment: 'success', service: 'info', lease: 'primary', other: 'info' }[t] || 'info')
const statusText = (s: string) => ({ pending: '待审核', reviewing: '审核中', approved: '已通过', rejected: '已驳回' }[s] || s)
const statusTag = (s: string) => ({ pending: 'warning', reviewing: 'primary', approved: 'success', rejected: 'danger' }[s] || 'info')
const riskTag = (l: string) => ({ low: 'success', medium: 'warning', high: 'danger', critical: 'danger' }[l] || 'info')

const handleFileChange = (file: UploadFile) => {
  if (file.raw) selectedFile.value = file.raw
}

const handleUpload = async () => {
  if (!uploadForm.title) { ElMessage.warning('请输入合同名称'); return }
  if (!selectedFile.value) { ElMessage.warning('请选择文件'); return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('title', uploadForm.title)
    fd.append('type', uploadForm.type)
    const res: any = await uploadContract(fd)
    if (res.code === 200) {
      ElMessage.success('上传成功')
      showUpload.value = false
      uploadForm.title = ''
      uploadForm.type = 'other'
      selectedFile.value = null
      search()
    }
  } finally { uploading.value = false }
}

const handleAnalyze = async (row: any) => {
  try {
    const res: any = await analyzeContract(row.id)
    if (res.code === 200) { ElMessage.success('AI审核完成'); search() }
  } catch (e) {}
}

const handleDelete = async (id: number) => {
  try {
    const res: any = await deleteContract(id)
    if (res.code === 200) { ElMessage.success('删除成功'); search() }
  } catch (e) {}
}

const search = async () => {
  loading.value = true
  try {
    const res: any = await getContractPage({ ...query })
    if (res.code === 200) {
      contracts.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally { loading.value = false }
}

onMounted(search)
</script>

<style scoped>
.contract-page { max-width: 1400px; }
.toolbar-card { margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>