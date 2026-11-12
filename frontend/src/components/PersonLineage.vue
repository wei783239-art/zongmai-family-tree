<template>
  <el-button
    type="primary"
    link
    :disabled="loading || props.disabled"
    @click="openLineage('ancestors')"
  >
    祖先
  </el-button>

  <el-button
    type="primary"
    link
    :disabled="loading || props.disabled"
    @click="openLineage('descendants')"
  >
    后代
  </el-button>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="680px"
    append-to-body
  >
    <el-skeleton v-if="loading" :rows="3" animated />

    <el-result
      v-else-if="loadFailed"
      icon="error"
      title="查询失败，请重新打开重试"
    />

    <el-empty
      v-else-if="members.length === 0"
      description="暂无记录"
    />

    <el-table v-else :data="members" row-key="id" border>
      <el-table-column prop="id" label="编号" width="90" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="generation" label="世代" />
      <el-table-column prop="birth_date" label="出生日期" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { listAncestors, listDescendants } from '@/api/family'

const props = defineProps({
  familyId: { type: String, required: true },
  person: { type: Object, required: true },
  disabled: { type: Boolean, default: false }
})

const dialogVisible = ref(false)
const loading = ref(false)
const loadFailed = ref(false)
const members = ref([])
const queryType = ref('ancestors')

const dialogTitle = computed(() => {
  const label = queryType.value === 'ancestors' ? '祖先' : '后代'
  return `${props.person.name} · ${label}`
})

const openLineage = async (type) => {
  if (loading.value || props.disabled) return

  queryType.value = type
  dialogVisible.value = true
  members.value = []
  loadFailed.value = false
  loading.value = true

  const targetFamilyId = props.familyId
  const targetPersonId = props.person.id

  try {
    if (type === 'ancestors') {
      members.value = await listAncestors(targetFamilyId, targetPersonId)
    } else {
      members.value = await listDescendants(targetFamilyId, targetPersonId)
    }
  } catch (error) {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}
</script>