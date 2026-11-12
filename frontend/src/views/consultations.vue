<template>
  <div>
    <h1>
      成员档案
      <span v-if="family"> · {{ family.name }}</span>
    </h1>

    <el-button @click="router.push('/back/dashboard')">
      返回家族总览
    </el-button>
    <el-button
      v-if="family && !loadFailed"
      type="primary"
      :disabled="loading || savingPerson"
      @click="openCreatePerson" 
    >
      新增成员
    </el-button>
    <FamilyRelations
      v-if="family && !loadFailed"
      :key="familyId"
      :family-id="familyId"
    />

    <div v-if="familyId" style="margin: 16px 0">
      <el-input
        v-model="searchText"
        placeholder="输入成员姓名"
        clearable
        style="width: 240px"
        :disabled="loading || savingPerson"
        @keyup.enter="searchPersons"
        @clear="resetSearch"
      />
      <el-button
        :disabled="loading || savingPerson"
        @click="searchPersons"
      >
        搜索
      </el-button>
      <el-button
        :disabled="loading || savingPerson"
        @click="resetSearch"
      >
        重置
      </el-button>
    </div>

    <el-empty v-if="!familyId" description="请先从家族总览选择家族" />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <el-result
      v-else-if="loadFailed"
      icon="error"
      title="成员资料加载失败"
      sub-title="请刷新页面重试"
    />

    <el-empty 
      v-else-if="persons.length === 0"
      :description="searchQuery.name ? '没有匹配的成员' : '该家族暂无成员'" 
    />

    <el-table v-else :data="persons" row-key="id" border>
      <el-table-column prop="name" label="姓名" />

      <el-table-column label="性别">
        <template #default="{ row }">
          {{ genderLabels[row.gender] || '未知' }}
        </template>
      </el-table-column>

      <el-table-column prop="generation" label="世代" />
      <el-table-column prop="birth_date" label="出生日期" />
      <el-table-column prop="death_date" label="逝世日期" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            :disabled="savingPerson"
            @click="openEditPerson(row)"
          >
            编辑
          </el-button>
          <el-button
            type="danger"
            link
            :disabled="savingPerson"
            @click="removePerson(row)"
          >
            删除
          </el-button>

          <PersonLineage
            :family-id="familyId"
            :person="row"
            :disabled="savingPerson"
          />
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="family && !loading && !loadFailed && total > 0"
      :current-page="searchQuery.page"
      :page-size="searchQuery.size"
      :page-sizes="[5, 10, 20]"
      :total="total"
      :disabled="savingPerson"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px"
      @update:current-page="changePage"
      @update:page-size="changePageSize"
    />
    <el-dialog
      v-model="personDialogVisible"
      :title="editingPerson ? '编辑成员' : '新增成员'" 
      width="460px"
      :close-on-click-modal="false"
      :close-on-press-escape="!savingPerson"
      :show-close="!savingPerson"
    >
      <el-form
        :model="personForm"
        label-position="top"
        :disabled="savingPerson"
      >
        <el-form-item label="姓名" required>
          <el-input v-model="personForm.name" placeholder="请输入成员姓名" />
        </el-form-item>

        <el-form-item label="性别">
          <el-select v-model="personForm.gender">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="世代">
          <el-input-number
            v-model="personForm.generation"
            :min="1"
            :max="2147483647"
            :precision="0"
            placeholder="可留空"
          />
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="personForm.birth_date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="未知可留空"
          />
        </el-form-item>

        <el-form-item label="逝世日期">
          <el-date-picker
            v-model="personForm.death_date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="在世或未知可留空"
          />
        </el-form-item>

        <el-form-item label="出生地">
          <el-input v-model="personForm.birth_place" />
        </el-form-item>

        <el-form-item label="人物简介">
          <el-input
            v-model="personForm.biography"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="savingPerson" @click="personDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="savingPerson" @click="submitPerson">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message-box/style/css'
import { getFamily, pagePersonsByFamily, createPerson, updatePerson, deletePerson } from '@/api/family'
import FamilyRelations from '@/components/FamilyRelations.vue'
import PersonLineage from '@/components/PersonLineage.vue'

const route = useRoute()
const router = useRouter()

const family = ref(null)
const persons = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const total = ref(0)
const searchText = ref('')
const searchQuery = ref({
  name: '',
  page: 1,
  size: 10
})

const searchPersons = () => {
  searchQuery.value = {
    ...searchQuery.value,
    name: searchText.value.trim(),
    page: 1
  }
}

const resetSearch = () => {
  searchText.value = ''
  searchQuery.value = {
    ...searchQuery.value,
    name: '',
    page: 1
  }
}

const changePage = (page) => {
  if (page === searchQuery.value.page) return
  searchQuery.value = { ...searchQuery.value, page }
}

const changePageSize = (size) => {
  if (size === searchQuery.value.size) return 
  searchQuery.value = { ...searchQuery.value, size, page: 1 }
}

const genderLabels = {
  MALE: '男',
  FEMALE: '女',
  UNKNOWN: '未知'
}

const familyId = computed(() => {
  const value = route.query.familyId
  if (typeof value === 'string' && /^[1-9]\d*$/.test(value)) {
    return value
  }
  return ''
})

watch([familyId, searchQuery], async ([id, search], previousValues, onCleanup) => {
  let cancelled = false
  onCleanup(() => {
    cancelled = true
  })

  family.value = null
  persons.value = []
  loadFailed.value = false
  loading.value = false

  if (!id) return

  loading.value = true
  try {
    const [familyData, personData] = await Promise.all([
      getFamily(id),
      pagePersonsByFamily(id, search.page, search.size, search.name)
    ])

    //切换家族后，忽略旧请求的结果
    if (!cancelled) {
      const lastPage = Math.max(
        1,
        Math.ceil(personData.total / search.size)
      )

      if (search.page > lastPage) {
        searchQuery.value = { ...search, page: lastPage }
        return
      }

      family.value = familyData
      persons.value = personData.records
      total.value = personData.total
    }
  } catch (error) {
    if (!cancelled) loadFailed.value = true
  } finally {
    if (!cancelled) loading.value = false
  }
}, { immediate: true })

const personDialogVisible = ref(false)
const savingPerson = ref(false)

const personForm = reactive({
  name: '',
  gender: 'UNKNOWN',
  generation: null,
  birth_date: null,
  death_date: null,
  birth_place: '',
  biography: ''
})

const editingPerson = ref(null)

const openCreatePerson = () => {
  editingPerson.value = null
  personForm.name = ''
  personForm.gender = 'UNKNOWN'
  personForm.generation = null
  personForm.birth_date = null
  personForm.death_date = null
  personForm.birth_place = ''
  personForm.biography = ''
  personDialogVisible.value = true
}

const openEditPerson = (person) => {
  editingPerson.value = { ...person }
  personForm.name = person.name
  personForm.gender = person.gender ?? 'UNKNOWN'
  personForm.generation = person.generation
  personForm.birth_date = person.birth_date ?? null
  personForm.death_date = person.death_date ?? null
  personForm.birth_place = person.birth_place ?? ''
  personForm.biography = person.biography ?? ''
  personDialogVisible.value = true
}

const submitPerson = async () => {
  if (savingPerson.value || !family.value) return

  const targetFamilyId = familyId.value
  const originalPerson = editingPerson.value
  const name = personForm.name.trim()

  if (!name) {
    ElMessage.warning('请输入成员姓名')
    return
  }

 const data = {
  name,
  gender: personForm.gender,
  generation: personForm.generation ?? null,
  birth_date: personForm.birth_date || null,
  death_date: personForm.death_date || null,
  birth_place: personForm.birth_place.trim() || null,
  biography: personForm.biography || null
}

  savingPerson.value = true
  try {
    if (originalPerson) {
      await updatePerson(targetFamilyId, originalPerson.id, data)
    } else {
      await createPerson(targetFamilyId, data)
    }

    personDialogVisible.value = false
    editingPerson.value = null
    ElMessage.success(originalPerson ? '成员修改成功' : '成员添加成功')

    if (familyId.value === targetFamilyId) {
      searchQuery.value = {
        ...searchQuery.value,
        page: originalPerson ? searchQuery.value.page : 1
      }
    }
  } catch (error) {
    // 请求层负责显示具体错误。
  } finally {
    savingPerson.value = false
  }
}

const removePerson = async (person) => {
  if (savingPerson.value || !family.value) return

  const targetFamilyId = familyId.value
  savingPerson.value = true

  try {
    await ElMessageBox.confirm(
      `确定要删除成员吗“${person.name}”吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (familyId.value !== targetFamilyId) return
    await deletePerson(targetFamilyId, person.id)

    if (familyId.value === targetFamilyId) {
      searchQuery.value = { ...searchQuery.value }
    }

    ElMessage.success('成员已删除')
  } catch (error) {
    // 取消确认时结束操作，接口错误由请求层显示
  } finally {
    savingPerson.value = false
  }
}
</script>