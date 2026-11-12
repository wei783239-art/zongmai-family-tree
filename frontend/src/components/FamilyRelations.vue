<template>
    <el-button :loading="loading" @click="openRelations">
        亲属关系
    </el-button>

    <el-dialog
      v-model="dialogVisible"
      title="亲属关系"
      width="760px"
      :close-on-click-modal="false"
      :close-on-press-escape="!savingRelation"
      :show-close="!savingRelation"
    >
    <el-form
      v-if="!loading && !loadFailed"
      :model="relationForm"
      :inline="true"
      :disabled="savingRelation"
    >
      <el-form-item label="父亲或母亲">
        <el-select
          v-model="relationForm.parentPersonId"
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="person in allPersons"
            :key="person.id"
            :label="person.name + ' (' + person.id + ')'"
            :value="person.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="子女">
        <el-select
          v-model="relationForm.childPersonId"
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="person in allPersons"
            :key="person.id"
            :label="person.name + ' (' + person.id + ')'"
            :value="person.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          :loading="savingRelation"
          @click="submitParentChild"
        >  
          添加亲子关系
        </el-button>
      </el-form-item>
    </el-form>

    <el-form
      v-if="!loading && !loadFailed"
      :model="spouseForm"
      :inline="true"
      :disabled="savingRelation"
    >  
      <el-form-item label="配偶一">
        <el-select
          v-model="spouseForm.personId"
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="person in allPersons"
            :key="person.id"
            :label="person.name + ' (' + person.id + ')'"
            :value="person.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="配偶二">
        <el-select
          v-model="spouseForm.spousePersonId"
          filterable
          style="width: 200px"
        >  
          <el-option
            v-for="person in allPersons"
            :key="person.id"
            :label="person.name + ' (' + person.id + ')'"
            :value="person.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          :loading="savingRelation"
          @click="submitSpouse"
        >
          添加配偶关系
        </el-button>
      </el-form-item>
    </el-form>
      <el-skeleton v-if="loading" :rows="3" animated />

      <el-result
        v-else-if="loadFailed"
        icon="error"
        title="关系加载失败，请重新打开窗口重试"
      />

      <el-empty
        v-else-if="relations.length === 0"
        description="暂无亲属关系"
      />

      <el-table v-else :data="relations" row-key="id" border>
        <el-table-column prop="id" label="关系编号" width="100" />

        <el-table-column label="成员一">
            <template #default="{ row }">
                {{ row.from_person_name }} ({{ row.from_person_id }})
            </template>
        </el-table-column>

        <el-table-column label="关系" width="190">
            <template #default="{ row }">
                {{ relationLabels[row.relation_type] || row.relation_type }}
            </template>
        </el-table-column>

        <el-table-column label="成员二 ">
            <template #default="{ row }">
                {{ row.to_person_name }} ({{ row.to_person_id }})
            </template>
        </el-table-column>

        <el-table-column label="操作" width="90">
            <template #default="{ row }">
                <el-button
                  type="danger"
                  link
                  :disabled="savingRelation || loading"
                  @click="removeRelation(row)"
                >
                  删除
                </el-button>
            </template>
        </el-table-column>
      </el-table>
    </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message-box/style/css'
import { listRelationsByFamily, listPersonsByFamily, createRelation, createSpouseRelation, deleteRelation } from '@/api/family'

const props = defineProps({
    familyId: {
        type: String,
        required: true
    }
})

const dialogVisible = ref(false)
const relations = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const allPersons = ref([])
const savingRelation = ref(false)
const relationForm = reactive({
    parentPersonId: null,
    childPersonId : null
})

const spouseForm = reactive({
    personId: null,
    spousePersonId: null
})
const relationLabels = {
    PARENT_CHILD: '亲子 (父母-> 子女)',
    SPOUSE: '配偶'
}

const openRelations = async () => {
    if (loading.value) return 
    dialogVisible.value = true
    relations.value = []
    allPersons.value = []
    loadFailed.value = false
    loading.value = true

    try {
        const [relationData, personData] = await Promise.all([
            listRelationsByFamily(props.familyId),
            listPersonsByFamily(props.familyId)
        ])

        relations.value = relationData
        allPersons.value = personData
    } catch (error) {
        loadFailed.value = true
    } finally {
        loading.value = false
    }
}

const submitParentChild = async () => {
    if (savingRelation.value || loading.value) return

    const { parentPersonId, childPersonId } = relationForm

    if (!parentPersonId || !childPersonId) {
        ElMessage.warning('请选择父亲或母亲以及子女')
        return
    }

    if (parentPersonId === childPersonId) {
        ElMessage.warning('父母和子女不能是同一个人')
        return
    }

    savingRelation.value = true
    try {
        await createRelation(props.familyId, { parentPersonId, childPersonId })

        ElMessage.success('亲子关系添加成功')
        relationForm.parentPersonId = null
        relationForm.childPersonId = null
        await openRelations()
    } catch (error) {
        //请求层负责显示重复关系，环路等错误
    } finally {
        savingRelation.value = false
    }
}

const submitSpouse = async () => {
    if (savingRelation.value || loading.value) return

    const { personId, spousePersonId } =spouseForm

    if (!personId || !spousePersonId) {
        ElMessage.warning('请选择配偶双方')
        return
    }

    if (personId === spousePersonId) {
        ElMessage.warning('配偶双方不能是同一个人')
        return
    }

    savingRelation.value = true
    try {
        await createSpouseRelation(props.familyId, { personId, spousePersonId })

        ElMessage.success('配偶关系添加成功')
        spouseForm.personId = null
        spouseForm.spousePersonId = null
        await openRelations()
    } catch (error) {
        //请求层负责显示具体错误
    } finally {
        savingRelation.value = false
    }
}

const removeRelation = async (relation) => {
    if (savingRelation.value || loading.value) return

    const targetFamilyId = props.familyId
    savingRelation.value = true

    try {
        await ElMessageBox.confirm(
            `确定删除“${relation.from_person_name}”与“${relation.to_person_name}”之间的关系吗？`,
            '删除亲属关系',
            {
                confirmButtonText: '删除',
                cancelButtonText: '取消',
                type: 'warning'
            }
        )

        await deleteRelation(targetFamilyId, relation.id) 

        relations.value = relations.value.filter(
            item => item.id !== relation.id
        )

        ElMessage.success('亲属关系已删除')
    } catch (error) {
        //取消确认时结束操作，接口错误由请求层显示
    } finally {
        savingRelation.value = false
    }
}
</script>