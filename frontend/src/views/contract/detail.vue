<template>
  <div class="contract-detail" v-loading="loading">
    <el-page-header @back="router.back()" :content="contract?.title || '合同详情'" class="page-header" />

    <el-row :gutter="20" class="content-row">
      <!-- Contract Info -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>合同信息</span>
              <div>
                <el-tag :type="statusTag(contract?.status)" size="small">{{ statusText(contract?.status) }}</el-tag>
                <el-tag v-if="contract?.riskLevel" :type="riskTag(contract?.riskLevel)" size="small" style="margin-left:8px">
                  风险: {{ contract?.riskLevel }} ({{ contract?.riskScore }}分)
                </el-tag>
              </div>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="合同名称">{{ contract?.title }}</el-descriptions-item>
            <el-descriptions-item label="合同类型">{{ typeLabel(contract?.type) }}</el-descriptions-item>
            <el-descriptions-item label="文件名">{{ contract?.fileName }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatSize(contract?.fileSize) }}</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ contract?.createdTime }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusText(contract?.status) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="content-card">
          <template #header><span>合同内容</span></template>
          <pre class="contract-text">{{ contract?.contentText || '暂无内容' }}</pre>
        </el-card>
      </el-col>

      <!-- Risk Analysis -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>AI 风险审核</span>
              <el-button type="primary" size="small" @click="runAnalysis" :loading="analyzing" :disabled="!contract">
                <el-icon><Search /></el-icon> 执行审核
              </el-button>
            </div>
          </template>
          <div v-if="risks.length === 0 && !analyzing" class="empty-hint">
            点击「执行审核」按钮进行 AI 风险分析
          </div>
          <div v-if="analyzing" class="analyzing-hint">
            <el-icon class="is-loading" size="24"><Loading /></el-icon>
            <span>AI 正在分析合同条款...</span>
          </div>
          <div v-for="risk in risks" :key="risk.id" class="risk-item">
            <div class="risk-header">
              <el-tag :type="riskTag(risk.riskLevel)" size="small">{{ risk.riskLevel }}</el-tag>
              <span class="risk-type">{{ risk.riskType }}</span>
            </div>
            <div class="risk-clause">条款：{{ risk.clause }}</div>
            <div class="risk-desc">{{ risk.description }}</div>
            <div class="risk-suggestion">
              <el-icon><WarningFilled /></el-icon>
              {{ risk.suggestion }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getContractDetail, getContractRisks } from '@/api/contract'
import { analyzeContract } from '@/api/ai'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const analyzing = ref(false)
const contract = ref<any>(null)
const risks = ref<any[]>([])

const typeLabel = (t: string) => ({ purchase: '采购', sale: '销售', NDA: '保密协议', employment: '劳动', service: '服务', lease: '租赁', other: '其他' }[t] || t)
const statusText = (s: string) => ({ pending: '待审核', reviewing: '审核中', approved: '已通过', rejected: '已驳回' }[s] || s)
const statusTag = (s: string) => ({ pending: 'warning', reviewing: 'primary', approved: 'success', rejected: 'danger' }[s])
const riskTag = (l: string) => ({ low: 'success', medium: 'warning', high: 'danger', critical: 'danger' }[l])

const formatSize = (bytes: number) => {
  if (!bytes) return '未知'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const [res1, res2] = await Promise.all([
      getContractDetail(id),
      getContractRisks(id)
    ])
    if (res1.code === 200) contract.value = res1.data
    if (res2.code === 200) risks.value = res2.data || []
  } catch (e) {
    ElMessage.error('加载合同详情失败')
  } finally {
    loading.value = false
  }
}

const runAnalysis = async () => {
  if (!contract.value) return
  analyzing.value = true
  try {
    const res = await analyzeContract(contract.value.id)
    if (res.code === 200) {
      ElMessage.success('审核完成')
      const riskRes = await getContractRisks(contract.value.id)
      if (riskRes.code === 200) risks.value = riskRes.data || []
      // Refresh contract for updated risk info
      const contractRes = await getContractDetail(contract.value.id)
      if (contractRes.code === 200) contract.value = contractRes.data
    }
  } catch (e) {
    // handled
  } finally {
    analyzing.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.contract-detail { max-width: 1400px; }
.page-header { margin-bottom: 20px; }
.content-row { margin-top: 16px; }
.content-card { margin-top: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.contract-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  max-height: 600px;
  overflow-y: auto;
  background: #fafafa;
  padding: 16px;
  border-radius: 4px;
}
.empty-hint { text-align: center; color: #909399; padding: 40px 0; }
.analyzing-hint { text-align: center; color: #409eff; padding: 40px 0; display: flex; align-items: center; justify-content: center; gap: 8px; }
.risk-item {
  border-bottom: 1px solid #ebeef5;
  padding: 12px 0;
}
.risk-item:last-child { border-bottom: none; }
.risk-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.risk-type { font-weight: 600; font-size: 14px; }
.risk-clause { font-size: 13px; color: #606266; margin-bottom: 4px; background: #f5f7fa; padding: 4px 8px; border-radius: 4px; }
.risk-desc { font-size: 13px; color: #606266; margin-bottom: 4px; }
.risk-suggestion { font-size: 13px; color: #e6a23c; display: flex; align-items: flex-start; gap: 4px; }
</style>