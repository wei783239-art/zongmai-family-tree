<template>
  <div>
    <div class="page-header">
      <h1>家族总览</h1>
      <div>
        <el-button
          type="primary"
          :disabled="loading || creating"
          @click="createDialogVisible = true"
        >
          创建家族
        </el-button>
        <el-button :loading="loading" @click="loadFamilies">
            刷新
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <el-result
      v-else-if="loadFailed"
      icon="error"
      title="家族列表加载失败"
      sub-title="请点击刷新重试"
    />

    <el-empty
      v-else-if="families.length === 0"
      description="暂无关联家族"
    />

    <el-table v-else :data="families" row-key="id" border>
      <el-table-column prop="id" label="编号" width="90" />
      <el-table-column prop="name" label="家族名称" />
      <el-table-column prop="surname" label="姓氏" />
      <el-table-column prop="ancestral_hall" label="堂号" />
      <el-table-column prop="hometown" label="祖籍" />
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="router.push({
                path: '/back/consultations',
                query: { familyId: String(row.id) }
              })"
            >
              进入家族
            </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog
      v-model="createDialogVisible"
      title="创建家族"
      width="460px"
      :close-on-click-modal="false"
      :close-on-press-escape="!creating"
      :show-close="!creating"
    >
      <el-form
        :model="familyForm"
        label-position="top"
        :disabled="creating"
      >
        <el-form-item label="家族名称" required>
            <el-input v-model="familyForm.name" placeholder="例如：林氏家族" />
        </el-form-item>
        <el-form-item label="姓氏">
            <el-input v-model="familyForm.surname" placeholder="例如：林" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="creating" @click="createDialogVisible = false">
            取消
        </el-button>
        <el-button type="primary" :loading="creating" @click="submitFamily">
            创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFamilies, createFamily } from '@/api/family'
import { useRouter } from 'vue-router'

const router = useRouter()
const families = ref([])
const loading = ref(false)
const loadFailed = ref(false)

const loadFamilies = async () => {
  if (loading.value) return

  loading.value = true
  loadFailed.value = false

  try {
    families.value = await listFamilies()
  } catch (error) {
    loadFailed.value = true
    // 请求层负责显示具体错误。
  } finally {
    loading.value = false
  }
}

const createDialogVisible = ref(false)
const creating = ref(false)
const familyForm = reactive({
    name: '',
    surname: ''
})

const submitFamily = async () => {
    if (creating.value) return

    const name = familyForm.name.trim()
    if (!name) {
        ElMessage.warning('请输入家族名称')
        return
    }

    creating.value = true
    try {
        await createFamily({
            name,
            surname: familyForm.surname.trim()
        })

        createDialogVisible.value = false
        familyForm.name = ''
        familyForm .surname = ''

        ElMessage.success('家族创建成功')
        await loadFamilies()
    } catch (error) {
        //请求层负责显示具体错误
    } finally {
        creating.value = false
    }   
}

onMounted(loadFamilies)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
</style>