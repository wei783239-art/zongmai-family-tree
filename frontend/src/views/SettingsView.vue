<template>
  <div>
    <FamilyHeader title="修谱审核与设置"
      ><el-button :loading="loading" @click="reload++"
        >刷新</el-button
      ></FamilyHeader
    >
    <section class="panel settings-panel">
      <div class="panel-head">
        <div>
          <p class="panel-title">修谱与家族治理</p>
          <p class="panel-sub">
            审核成员申请、维护账号角色、设置始祖并导出家族资料
          </p>
        </div>
        <span class="panel-seal">修谱</span>
      </div>
      <el-alert
        v-if="failed"
        title="部分设置加载失败，请刷新重试"
        type="error"
        :closable="false"
      />
      <el-tabs v-model="tab">
      <el-tab-pane
        v-if="familyId"
        :label="canManage ? '待审核与历史申请' : '我的申请'"
        name="requests"
      >
        <el-table :data="requests" row-key="id" border
          ><el-table-column prop="id" label="编号" width="80" /><el-table-column
            prop="username"
            label="申请人"
          /><el-table-column label="类型"
            ><template #default="{ row }">{{
              kindLabels[row.kind] || row.kind
            }}</template></el-table-column
          ><el-table-column
            prop="target_id"
            label="人物编号"
            width="100"
          /><el-table-column label="状态"
            ><template #default="{ row }">{{
              statusLabels[row.status]
            }}</template></el-table-column
          ><el-table-column
            prop="review_note"
            label="审核意见"
          /><el-table-column label="操作" min-width="190"
            ><template #default="{ row }"
              ><el-button link @click="showRequest(row)">查看内容</el-button
              ><template v-if="canManage && row.status === 'PENDING'"
                ><el-button
                  link
                  type="primary"
                  :disabled="busy"
                  @click="review(row, true)"
                  >通过</el-button
                ><el-button
                  link
                  type="danger"
                  :disabled="busy"
                  @click="review(row, false)"
                  >驳回</el-button
                ></template
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane
        v-if="familyId && canManage"
        label="账号与角色"
        name="members"
      >
        <el-form :inline="true" :disabled="busy"
          ><el-form-item label="已注册用户名"
            ><el-input v-model="memberForm.username" /></el-form-item
          ><el-form-item label="角色"
            ><el-select v-model="memberForm.role" style="width: 160px"
              ><el-option label="普通成员" value="MEMBER" /><el-option
                label="家族管理员"
                value="FAMILY_ADMIN" /></el-select></el-form-item
          ><el-button type="primary" :loading="busy" @click="saveMember"
            >关联或更新角色</el-button
          ></el-form
        >
        <el-table :data="members" row-key="id" border
          ><el-table-column prop="id" label="账号编号" /><el-table-column
            prop="username"
            label="用户名"
          /><el-table-column label="角色"
            ><template #default="{ row }">{{
              row.role === "FAMILY_ADMIN" ? "家族管理员" : "普通成员"
            }}</template></el-table-column
          ><el-table-column prop="status" label="账号状态" /><el-table-column
            label="操作"
            ><template #default="{ row }"
              ><el-button
                link
                @click="
                  Object.assign(memberForm, {
                    username: row.username,
                    role: row.role,
                  })
                "
                >修改角色</el-button
              ><el-button
                link
                type="danger"
                :disabled="busy"
                @click="removeMember(row)"
                >移出家族</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane
        v-if="familyId && canManage"
        label="始祖与数据导出"
        name="data"
      >
        <el-form :inline="true" :disabled="busy"
          ><el-form-item label="家族始祖"
            ><el-select
              v-model="ancestorId"
              clearable
              filterable
              style="width: 260px"
              ><el-option
                v-for="p in people"
                :key="p.id"
                :label="`${p.name}（${p.id}）`"
                :value="p.id" /></el-select></el-form-item
          ><el-button type="primary" :loading="busy" @click="saveAncestor"
            >保存始祖设置</el-button
          ></el-form
        >
        <p>
          JSON
          导出包含家族、人物、关系、事件及档案文件内容。数据库完整恢复请按部署说明备份
          MySQL。
        </p>
        <div class="workspace-actions">
          <el-button :loading="busy" @click="download('csv')"
            >导出成员 CSV</el-button
          ><el-button :loading="busy" @click="download('json')"
            >导出家族 JSON</el-button
          >
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="familyId && canManage" label="操作日志" name="audit"
        ><el-table :data="audit" row-key="id" max-height="600"
          ><el-table-column
            prop="created_at"
            label="时间"
            min-width="190" /><el-table-column
            prop="username"
            label="操作人" /><el-table-column
            prop="action"
            label="方法"
            width="100" /><el-table-column
            prop="target"
            label="操作对象"
            min-width="300" /></el-table
      ></el-tab-pane>
      <el-tab-pane
        v-if="adminStore.user?.systemAdmin"
        label="系统账号"
        name="system"
        ><el-table :data="users" row-key="id"
          ><el-table-column prop="id" label="编号" /><el-table-column
            prop="username"
            label="用户名"
          /><el-table-column prop="status" label="状态" /><el-table-column
            label="操作"
            ><template #default="{ row }"
              ><el-button
                link
                :type="row.status === 'ACTIVE' ? 'danger' : 'primary'"
                :disabled="busy || row.id === adminStore.user.id"
                @click="setStatus(row)"
                >{{ row.status === "ACTIVE" ? "停用" : "启用" }}</el-button
              ></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      >
      </el-tabs>
    </section>
    <el-dialog v-model="requestDialog" title="申请内容" width="min(660px,94vw)">
      <pre class="request-json">{{ requestText }}</pre>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import FamilyHeader from "@/components/FamilyHeader.vue";
import { useFamilyContext } from "@/composables/useFamilyContext";
import { useAdminStore } from "@/stores/admin";
import {
  familyRequest,
  downloadFamily,
  listSystemUsers,
  changeUserStatus,
} from "@/api/workspace";
const { familyId, family, families, canManage, refreshFamilies, selectFamily } =
  useFamilyContext();
const adminStore = useAdminStore();
const tab = ref("requests"),
  loading = ref(false),
  busy = ref(false),
  failed = ref(false),
  reload = ref(0);
const requests = ref([]),
  members = ref([]),
  people = ref([]),
  audit = ref([]),
  users = ref([]),
  ancestorId = ref(null),
  requestDialog = ref(false),
  requestText = ref("");
const memberForm = reactive({ username: "", role: "MEMBER" });
const kindLabels = {
  PERSON_UPDATE: "资料修改",
  PARENT_CHILD: "亲子关系",
  SPOUSE: "配偶关系",
  ADOPTIVE_PARENT_CHILD: "收养关系",
};
const statusLabels = {
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已驳回",
};
watch(
  [familyId, canManage, reload],
  async ([id, manage], previous, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });
    loading.value = true;
    failed.value = false;
    requests.value = [];
    members.value = [];
    audit.value = [];
    people.value = [];
    const jobs = [];
    if (id)
      jobs.push(
        familyRequest(id, "/requests").then((result) => {
          if (!cancelled) requests.value = result;
        }),
      );
    if (id && manage) {
      jobs.push(
        familyRequest(id, "/members").then((result) => {
          if (!cancelled) members.value = result;
        }),
      );
      jobs.push(
        familyRequest(id, "/persons").then((result) => {
          if (!cancelled) people.value = result;
        }),
      );
      jobs.push(
        familyRequest(id, "/audit").then((result) => {
          if (!cancelled) audit.value = result;
        }),
      );
    }
    if (adminStore.user?.systemAdmin)
      jobs.push(
        listSystemUsers().then((result) => {
          if (!cancelled) users.value = result;
        }),
      );
    const results = await Promise.allSettled(jobs);
    if (!cancelled) {
      failed.value = results.some((result) => result.status === "rejected");
      ancestorId.value = family.value?.ancestor_person_id || null;
      loading.value = false;
      if (!manage && ["members", "audit", "data"].includes(tab.value))
        tab.value = "requests";
    }
  },
  { immediate: true },
);
function showRequest(row) {
  try {
    requestText.value = JSON.stringify(JSON.parse(row.payload_json), null, 2);
  } catch {
    requestText.value = row.payload_json;
  }
  requestDialog.value = true;
}
async function mutate(path, method, data) {
  if (busy.value) return;
  busy.value = true;
  try {
    await familyRequest(familyId.value, path, { method, data });
    await refreshFamilies();
    if (!families.value.some((f) => String(f.id) === familyId.value)) {
      if (families.value.length) await selectFamily(families.value[0].id);
    } else reload.value++;
    ElMessage.success("操作已完成");
  } catch {
    /* 全局提示 */
  } finally {
    busy.value = false;
  }
}
function saveMember() {
  if (!memberForm.username.trim()) return ElMessage.warning("请输入用户名");
  return mutate("/members", "put", {
    ...memberForm,
    username: memberForm.username.trim(),
  });
}
async function removeMember(row) {
  try {
    await ElMessageBox.confirm(`将“${row.username}”移出家族？`, "移出家族", {
      type: "warning",
    });
    await mutate(`/members/${row.id}`, "delete");
  } catch {
    /* 取消 */
  }
}
function saveAncestor() {
  return mutate(
    "/ancestor",
    ancestorId.value ? "put" : "delete",
    ancestorId.value ? { personId: ancestorId.value } : undefined,
  );
}
async function review(row, approve) {
  try {
    const result = await ElMessageBox.prompt(
      "填写审核意见（可选）",
      approve ? "通过申请" : "驳回申请",
      {
        inputType: "textarea",
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        inputValidator: (value) =>
          !value || value.length <= 500 || "审核意见不能超过500字",
      },
    );
    await mutate(`/requests/${row.id}/review`, "post", {
      approve,
      note: result.value || null,
    });
  } catch {
    /* 取消 */
  }
}
async function download(format) {
  if (busy.value) return;
  busy.value = true;
  try {
    await downloadFamily(familyId.value, format);
  } catch {
    /* 全局提示 */
  } finally {
    busy.value = false;
  }
}
async function setStatus(row) {
  if (busy.value) return;
  try {
    await ElMessageBox.confirm(
      `确定${row.status === "ACTIVE" ? "停用" : "启用"}“${row.username}”？`,
      "账号状态",
      { type: "warning" },
    );
    busy.value = true;
    await changeUserStatus(
      row.id,
      row.status === "ACTIVE" ? "DISABLED" : "ACTIVE",
    );
    reload.value++;
  } catch {
    /* 取消或全局提示 */
  } finally {
    busy.value = false;
  }
}
</script>

<style scoped>
.settings-panel {
  padding-top: 18px;
}
.settings-panel :deep(.el-tabs__content) {
  padding-top: 4px;
}
.settings-panel :deep(.el-form--inline .el-form-item) {
  margin-bottom: 14px;
}
.settings-panel :deep(.el-table) {
  margin-top: 4px;
}
.settings-panel :deep(.el-tabs__header) {
  margin-bottom: 18px;
}
@media (max-width: 700px) {
  .settings-panel {
    padding: 16px 12px;
  }
}
</style>

