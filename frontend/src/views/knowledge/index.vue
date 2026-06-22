<template>
  <div class="knowledge-page">
    <el-card class="toolbar-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input v-model="query.keyword" placeholder="搜索知识文档" clearable @clear="search" @keyup.enter="search">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="query.category" placeholder="分类" clearable @change="search" style="width:100%">
            <el-option label="合同法" value="contract_law" />
            <el-option label="劳动法" value="labor_law" />
            <el-option label="知识产权" value="ip" />
            <el-option label="公司法" value="company_law" />
            <el-option label="诉讼程序" value="procedure" />
            <el-option label="合规风控" value="compliance" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-col>
        <el-col :span="12" style="text-align:right">
          <el-button type="primary" @click="showDialog = true; dialogType = 'create'">
            <el-icon><Plus /></el-icon> 新增知识
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Knowledge List -->
    <el-card>
      <el-table :data="docs" stripe v-loading="loading" style="width:100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" width="200">
          <template #default="{ row }">
            <el-tag v-for="tag in (row.tags || '').split(',')" :key="tag" size="small" style="margin-right:4px;margin-bottom:2px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该知识条目？" @confirm="handleDelete(row.id)">
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

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showDialog" :title="dialogType === 'create' ? '新增知识' : '编辑知识'" width="700px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
            <el-option label="合同法" value="contract_law" />
            <el-option label="劳动法" value="labor_law" />
            <el-option label="知识产权" value="ip" />
            <el-option label="公司法" value="company_law" />
            <el-option label="诉讼程序" value="procedure" />
            <el-option label="合规风控" value="compliance" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="12" placeholder="请输入知识内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getKnowledgePage, createKnowledge, updateKnowledge, deleteKnowledge } from '@/api/knowledge'

const loading = ref(false)
const saving = ref(false)
const docs = ref<any[]>([])
const total = ref(0)
const showDialog = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const query = reactive({ page: 1, size: 10, keyword: '', category: '' })
const form = reactive({ title: '', category: '', content: '', tags: '' })

const categoryLabel = (c: string) => ({
  contract_law: '合同法', labor_law: '劳动法', ip: '知识产权',
  company_law: '公司法', procedure: '诉讼程序', compliance: '合规风控', other: '其他'
}[c] || c)

const handleEdit = (row: any) => {
  dialogType.value = 'edit'
  editingId.value = row.id
  form.title = row.title
  form.category = row.category
  form.content = row.content
  form.tags = row.tags || ''
  showDialog.value = true
}

const handleSave = async () => {
  if (!form.title || !form.content) { ElMessage.warning('请填写标题和内容'); return }
  saving.value = true
  try {
    let res
    if (dialogType.value === 'create') {
      res = await createKnowledge({ ...form })
    } else {
      res = await updateKnowledge({ id: editingId.value, ...form })
    }
    if (res.code === 200) {
      ElMessage.success(dialogType.value === 'create' ? '创建成功' : '更新成功')
      showDialog.value = false
      form.title = ''; form.category = ''; form.content = ''; form.tags = ''
      search()
    }
  } catch (e) {
    // handled
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    const res = await deleteKnowledge(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      search()
    }
  } catch (e) { /* handled */ }
}

const search = async () => {
  loading.value = true
  try {
    const res = await getKnowledgePage({ ...query })
    if (res.code === 200) {
      docs.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

onMounted(search)
</script>

<style scoped>
.knowledge-page { max-width: 1400px; }
.toolbar-card { margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>