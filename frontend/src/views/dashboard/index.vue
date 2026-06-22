<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-value">{{ stat.value }}</div>
            </div>
            <el-icon :size="48" :color="stat.color">
              <component :is="stat.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="action-card">
      <template #header><span class="card-title">快捷操作</span></template>
      <el-row :gutter="20">
        <el-col :span="6" v-for="action in quickActions" :key="action.label">
          <el-button :type="action.type" class="action-btn" @click="action.handler">
            <el-icon :size="24"><component :is="action.icon" /></el-icon>
            <span>{{ action.label }}</span>
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="recent-card">
      <template #header><span class="card-title">最近合同</span></template>
      <el-table :data="recentContracts" stripe style="width: 100%" v-loading="tableLoading">
        <el-table-column prop="title" label="合同名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="router.push('/contract/' + row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { Document, Clock, WarningFilled, Collection, Upload, ChatLineSquare, Edit } from '@element-plus/icons-vue'
import { getContractPage } from '@/api/contract'

const router = useRouter()
const tableLoading = ref(false)
const recentContracts = ref([])

const stats = ref([
  { label: '合同总数', value: 0, icon: markRaw(Document), color: '#409eff' },
  { label: '待审核', value: 0, icon: markRaw(Clock), color: '#e6a23c' },
  { label: '高风险', value: 0, icon: markRaw(WarningFilled), color: '#f56c6c' },
  { label: '知识条目', value: 0, icon: markRaw(Collection), color: '#67c23a' }
])

const quickActions = [
  { label: '上传合同', icon: markRaw(Upload), type: 'primary' as const, handler: () => router.push('/contract') },
  { label: '智能对话', icon: markRaw(ChatLineSquare), type: 'success' as const, handler: () => router.push('/chat') },
  { label: '知识库', icon: markRaw(Collection), type: 'warning' as const, handler: () => router.push('/knowledge') },
  { label: '生成合同', icon: markRaw(Edit), type: 'info' as const, handler: () => router.push('/chat') }
]

const statusMap: Record<string, string> = { pending: '待审核', reviewing: '审核中', approved: '已通过', rejected: '已驳回' }

const getTypeTag = (t: string) => {
  const map: Record<string, string> = { purchase: 'primary', sale: 'primary', NDA: 'warning', employment: 'success', service: 'info', lease: 'primary', other: 'info' }
  return map[t] || 'info'
}

const getStatusTag = (status: string) => {
  const map: Record<string, string> = { pending: 'warning', reviewing: 'primary', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const riskTag = (level: string) => {
  const map: Record<string, string> = { low: 'success', medium: 'warning', high: 'danger', critical: 'danger' }
  return map[level] || 'info'
}

const loadData = async () => {
  tableLoading.value = true
  try {
    const res: any = await getContractPage({ page: 1, size: 5 })
    if (res.code === 200) {
      recentContracts.value = res.data.records || []
      stats.value[0].value = res.data.total || 0
    }
  } catch (e) {
    // ignore
  } finally {
    tableLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.dashboard { max-width: 1200px; }
.stats-row { margin-bottom: 20px; }
.stat-card { cursor: pointer; }
.stat-content { display: flex; align-items: center; justify-content: space-between; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.action-card, .recent-card { margin-bottom: 20px; }
.card-title { font-size: 16px; font-weight: 600; }
.action-btn {
  width: 100%;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
}
</style>