<template>
  <el-button @click="open()">{{
    canManage ? "维护亲属关系" : "申请添加关系"
  }}</el-button>
  <el-dialog
    v-model="visible"
    title="亲属关系"
    width="min(850px, 95vw)"
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :show-close="!saving"
  >
    <el-form :inline="true" :disabled="saving || loading">
      <el-form-item label="关系类型"
        ><el-select v-model="kind" style="width: 160px"
          ><el-option label="父母到子女" value="PARENT_CHILD" /><el-option
            label="收养父母到子女"
            value="ADOPTIVE_PARENT_CHILD" /><el-option
            label="配偶"
            value="SPOUSE" /></el-select
      ></el-form-item>
      <el-form-item :label="kind === 'SPOUSE' ? '成员一' : '父亲或母亲'"
        ><el-select v-model="from" filterable style="width: 200px"
          ><el-option
            v-for="p in people"
            :key="p.id"
            :value="p.id"
            :label="`${p.name}（${p.id}）`" /></el-select
      ></el-form-item>
      <el-form-item :label="kind === 'SPOUSE' ? '成员二' : '子女'"
        ><el-select v-model="to" filterable style="width: 200px"
          ><el-option
            v-for="p in people"
            :key="p.id"
            :value="p.id"
            :label="`${p.name}（${p.id}）`" /></el-select
      ></el-form-item>
      <el-button type="primary" :loading="saving" @click="save">{{
        canManage ? "添加关系" : "提交审核"
      }}</el-button>
    </el-form>
    <el-skeleton v-if="loading" :rows="3" animated />
    <el-alert
      v-else-if="failed"
      title="加载失败，请关闭后重新打开"
      type="error"
    />
    <el-table v-else :data="relations" max-height="420" row-key="id">
      <el-table-column prop="from_person_name" label="成员一" /><el-table-column
        label="关系"
        ><template #default="{ row }">{{
          labels[row.relation_type]
        }}</template></el-table-column
      ><el-table-column prop="to_person_name" label="成员二" />
      <el-table-column v-if="canManage" label="操作" width="90"
        ><template #default="{ row }"
          ><el-button link type="danger" :disabled="saving" @click="remove(row)"
            >删除</el-button
          ></template
        ></el-table-column
      >
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { familyRequest } from "@/api/workspace";
const props = defineProps({
  familyId: { type: String, required: true },
  canManage: Boolean,
});
const emit = defineEmits(["changed"]);
const visible = ref(false),
  loading = ref(false),
  saving = ref(false),
  failed = ref(false);
const people = ref([]),
  relations = ref([]),
  from = ref(null),
  to = ref(null),
  kind = ref("PARENT_CHILD");
const labels = {
  PARENT_CHILD: "亲子",
  ADOPTIVE_PARENT_CHILD: "收养",
  SPOUSE: "配偶",
};
async function load() {
  loading.value = true;
  failed.value = false;
  try {
    [people.value, relations.value] = await Promise.all([
      familyRequest(props.familyId, "/persons"),
      familyRequest(props.familyId, "/relations"),
    ]);
  } catch {
    failed.value = true;
  } finally {
    loading.value = false;
  }
}
async function open(personId = null) {
  from.value = personId;
  to.value = null;
  visible.value = true;
  await load();
}
async function save() {
  if (saving.value || loading.value) return;
  if (!from.value || !to.value || from.value === to.value)
    return ElMessage.warning("请选择两位不同的成员");
  saving.value = true;
  try {
    if (!props.canManage)
      await familyRequest(props.familyId, "/requests", {
        method: "post",
        data: {
          kind: kind.value,
          payload: { fromPersonId: from.value, toPersonId: to.value },
        },
      });
    else {
      const spouse = kind.value === "SPOUSE";
      const path = spouse
        ? "/relations/spouse"
        : kind.value === "ADOPTIVE_PARENT_CHILD"
          ? "/relations/adoptive"
          : "/relations";
      await familyRequest(props.familyId, path, {
        method: "post",
        data: spouse
          ? { personId: from.value, spousePersonId: to.value }
          : { parentPersonId: from.value, childPersonId: to.value },
      });
    }
    ElMessage.success(props.canManage ? "关系已添加" : "申请已提交");
    to.value = null;
    await load();
    emit("changed");
  } catch {
    /* 全局提示 */
  } finally {
    saving.value = false;
  }
}
async function remove(row) {
  if (saving.value) return;
  try {
    await ElMessageBox.confirm(
      `删除“${row.from_person_name}”与“${row.to_person_name}”的关系？`,
      "删除关系",
      { type: "warning" },
    );
    saving.value = true;
    await familyRequest(props.familyId, `/relations/${row.id}`, {
      method: "delete",
    });
    await load();
    emit("changed");
  } catch {
    /* 取消或全局错误提示 */
  } finally {
    saving.value = false;
  }
}
defineExpose({ open });
</script>
