<template>
  <el-dialog
    v-model="visible"
    :title="person ? (canManage ? '编辑成员' : '提交资料修改申请') : '新增成员'"
    width="min(620px, 94vw)"
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :show-close="!saving"
  >
    <el-form
      :model="form"
      label-position="top"
      :disabled="saving"
      @submit.prevent="save"
    >
      <div class="form-grid">
        <el-form-item label="姓名" required
          ><el-input v-model="form.name" maxlength="120"
        /></el-form-item>
        <el-form-item label="性别"
          ><el-select v-model="form.gender"
            ><el-option label="男" value="MALE" /><el-option
              label="女"
              value="FEMALE" /><el-option
              label="未知"
              value="UNKNOWN" /></el-select
        ></el-form-item>
        <el-form-item label="世代"
          ><el-input-number
            v-model="form.generation"
            :min="1"
            :max="2147483647"
            :precision="0"
        /></el-form-item>
        <el-form-item label="字辈"
          ><el-input v-model="form.nickname" maxlength="80"
        /></el-form-item>
        <el-form-item label="出生日期"
          ><el-date-picker
            v-model="form.birth_date"
            value-format="YYYY-MM-DD"
            placeholder="未知可留空"
        /></el-form-item>
        <el-form-item label="逝世日期"
          ><el-date-picker
            v-model="form.death_date"
            value-format="YYYY-MM-DD"
            placeholder="在世或未知可留空"
        /></el-form-item>
        <el-form-item label="出生地"
          ><el-input v-model="form.birth_place" maxlength="160"
        /></el-form-item>
        <el-form-item label="籍贯"
          ><el-input v-model="form.hometown" maxlength="160"
        /></el-form-item>
      </div>
      <el-form-item label="可见范围" v-if="canManage"
        ><el-select v-model="form.visibility"
          ><el-option label="家族可见" value="FAMILY" /><el-option
            label="仅管理员"
            value="PRIVATE" /><el-option
            label="公开标记（访问仍需家族授权）"
            value="PUBLIC" /></el-select
      ></el-form-item>
      <el-form-item label="头像"
        ><el-select
          v-model="form.avatar_url"
          clearable
          placeholder="从家族相册选择图片"
          ><el-option
            v-for="item in images"
            :key="item.id"
            :label="item.title"
            :value="`/api/families/${familyId}/media/${item.id}/file`" /></el-select
      ></el-form-item>
      <el-form-item label="人物简介"
        ><el-input
          v-model="form.biography"
          type="textarea"
          :rows="4"
          maxlength="20000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">{{
        canManage ? "保存" : "提交审核"
      }}</el-button></template
    >
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createPerson, updatePerson } from "@/api/family";
import { familyRequest } from "@/api/workspace";
const props = defineProps({
  familyId: { type: String, required: true },
  canManage: Boolean,
});
const emit = defineEmits(["saved"]);
const visible = ref(false);
const saving = ref(false);
const person = ref(null);
const images = ref([]);
const blank = () => ({
  name: "",
  gender: "UNKNOWN",
  generation: null,
  nickname: "",
  hometown: "",
  birth_place: "",
  biography: "",
  birth_date: null,
  death_date: null,
  visibility: "FAMILY",
  avatar_url: null,
});
const form = reactive(blank());
async function open(value = null) {
  person.value = value;
  Object.assign(form, blank());
  if (value)
    for (const key of Object.keys(form)) form[key] = value[key] ?? form[key];
  visible.value = true;
  images.value = [];
  try {
    images.value = (await familyRequest(props.familyId, "/media")).filter(
      (item) => item.content_type.startsWith("image/"),
    );
  } catch {
    /* 全局提示 */
  }
}
async function save() {
  if (saving.value) return;
  if (!form.name.trim()) return ElMessage.warning("请输入姓名");
  if (form.birth_date && form.death_date && form.death_date < form.birth_date)
    return ElMessage.warning("逝世日期不能早于出生日期");
  saving.value = true;
  try {
    const data = {
      ...form,
      name: form.name.trim(),
      birth_date: form.birth_date || null,
      death_date: form.death_date || null,
      avatar_url: form.avatar_url || null,
    };
    if (!props.canManage) {
      await familyRequest(props.familyId, "/requests", {
        method: "post",
        data: {
          kind: "PERSON_UPDATE",
          targetId: person.value.id,
          payload: data,
        },
      });
    } else if (person.value)
      await updatePerson(props.familyId, person.value.id, data);
    else await createPerson(props.familyId, data);
    visible.value = false;
    ElMessage.success(props.canManage ? "成员已保存" : "申请已提交");
    emit("saved");
  } catch {
    /* 全局提示 */
  } finally {
    saving.value = false;
  }
}
defineExpose({ open });
</script>
