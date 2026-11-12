<template>
  <div>
    <FamilyHeader title="成员档案">
      <el-button v-if="canManage" type="primary" @click="editor.open()"
        >新增成员</el-button
      >
      <RelationEditor
        v-if="familyId"
        :key="familyId"
        :family-id="familyId"
        :can-manage="canManage"
        @changed="reload++"
      />
    </FamilyHeader>

    <template v-if="familyId">
      <section class="panel filter-panel">
        <div class="filter-caption">
          <span class="filter-rule"></span>
          检索档案
        </div>
        <el-form :inline="true" class="filter-form" @submit.prevent="search">
          <el-form-item label="姓名">
            <el-input
              v-model="filters.name"
              placeholder="输入姓名"
              clearable
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="世代">
            <el-input-number
              v-model="filters.generation"
              :min="1"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="出生年份">
            <el-input-number
              v-model="filters.birthYear"
              :min="1"
              :max="9999"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" :disabled="loading" @click="search"
              >搜索</el-button
            >
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="panel member-panel">
        <div class="panel-head">
          <div>
            <p class="panel-title">成员名录</p>
            <p class="panel-sub">
              共 {{ total }} 位成员，按族谱档案整理；展开行可查看籍贯、字辈与简介
            </p>
          </div>
          <span class="panel-seal">宗谱</span>
        </div>

        <el-skeleton v-if="loading" :rows="5" animated />
        <el-result v-else-if="failed" icon="error" title="成员加载失败">
          <template #extra>
            <el-button @click="reload++">重试</el-button>
          </template>
        </el-result>
        <el-table v-else :data="records" row-key="id" border class="member-table">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="member-detail">
                <p>
                  出生地 · {{ row.birth_place || "未填写" }}　籍贯 ·
                  {{ row.hometown || "未填写" }}　字辈 ·
                  {{ row.nickname || "未填写" }}
                </p>
                <p class="biography">{{ row.biography || "暂无简介" }}</p>
                <el-image
                  v-if="row.avatar_url"
                  :src="row.avatar_url"
                  class="person-avatar"
                  fit="cover"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="id" label="编号" width="80" />
          <el-table-column prop="name" label="姓名" min-width="110" />
          <el-table-column label="性别" width="80">
            <template #default="{ row }">
              <span class="gender-text">{{
                { MALE: "男", FEMALE: "女" }[row.gender] || "未知"
              }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="generation" label="世代" width="80" />
          <el-table-column
            prop="birth_date"
            label="出生日期"
            min-width="115"
          />
          <el-table-column prop="death_date" label="逝世日期" min-width="115" />
          <el-table-column label="操作" min-width="270">
            <template #default="{ row }">
              <el-button link type="primary" @click="editor.open(row)">{{
                canManage ? "编辑" : "申请修改"
              }}</el-button>
              <el-button
                v-if="canManage"
                link
                type="danger"
                :disabled="deleting"
                @click="remove(row)"
                >删除</el-button
              >
              <PersonLineage
                :family-id="familyId"
                :person="row"
                :disabled="deleting"
              />
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="member-pagination"
          :current-page="query.page"
          :page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @update:current-page="(page) => (query = { ...query, page })"
          @update:page-size="(size) => (query = { ...query, size, page: 1 })"
        />
      </section>

      <PersonEditor
        :key="familyId"
        ref="editor"
        :family-id="familyId"
        :can-manage="canManage"
        @saved="
          reload++;
          refreshFamilies();
        "
      />
    </template>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import FamilyHeader from "@/components/FamilyHeader.vue";
import PersonEditor from "@/components/PersonEditor.vue";
import RelationEditor from "@/components/RelationEditor.vue";
import PersonLineage from "@/components/PersonLineage.vue";
import { useFamilyContext } from "@/composables/useFamilyContext";
import { familyRequest } from "@/api/workspace";

const { familyId, canManage, refreshFamilies } = useFamilyContext();
const editor = ref(null),
  records = ref([]),
  total = ref(0),
  loading = ref(false),
  failed = ref(false),
  deleting = ref(false),
  reload = ref(0);
const filters = reactive({ name: "", generation: null, birthYear: null });
const query = ref({
  name: "",
  generation: null,
  birthYear: null,
  page: 1,
  size: 10,
});
function search() {
  query.value = {
    ...query.value,
    ...filters,
    name: filters.name.trim(),
    page: 1,
  };
}
function reset() {
  Object.assign(filters, { name: "", generation: null, birthYear: null });
  search();
}
watch(
  [familyId, query, reload],
  async ([id, params], previous, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });
    records.value = [];
    total.value = 0;
    failed.value = false;
    loading.value = Boolean(id);
    if (!id) return;
    if (previous?.[0] && previous[0] !== id && params.page !== 1) {
      query.value = { ...params, page: 1 };
      return;
    }
    try {
      const result = await familyRequest(id, "/persons/page", { params });
      if (!cancelled) {
        const last = Math.max(1, Math.ceil(result.total / params.size));
        if (params.page > last) {
          query.value = { ...params, page: last };
          return;
        }
        records.value = result.records;
        total.value = result.total;
      }
    } catch {
      if (!cancelled) failed.value = true;
    } finally {
      if (!cancelled) loading.value = false;
    }
  },
  { immediate: true },
);
async function remove(row) {
  if (deleting.value) return;
  const id = familyId.value;
  try {
    await ElMessageBox.confirm("删除“" + row.name + "”？", "删除成员", {
      type: "warning",
    });
    deleting.value = true;
    await familyRequest(id, "/persons/" + row.id, { method: "delete" });
    reload.value++;
    await refreshFamilies();
    ElMessage.success("成员已删除");
  } catch {
    /* 取消或全局提示 */
  } finally {
    deleting.value = false;
  }
}
</script>

<style scoped>
.filter-panel {
  padding: 16px 22px 6px;
  margin-bottom: 18px;
}
.filter-caption {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 12px;
  font-family: var(--serif);
  font-size: 12.5px;
  letter-spacing: 0.16em;
  color: var(--ink-400);
}
.filter-rule {
  width: 18px;
  height: 1px;
  background: var(--bronze);
}
.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 2px 14px;
}
.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}
.filter-form :deep(.el-input) {
  width: 180px;
}
.filter-form :deep(.el-input-number) {
  width: 142px;
}
.filter-actions :deep(.el-form-item__content) {
  gap: 8px;
}
.member-panel {
  padding-bottom: 18px;
}
.member-detail {
  padding: 18px 22px;
  background: #fbf7ee;
  border-radius: 10px;
}
.member-detail p {
  margin: 0 0 8px;
}
.member-pagination {
  margin-top: 18px;
}
.gender-text {
  color: var(--ink-500);
}
@media (max-width: 640px) {
  .filter-form :deep(.el-input),
  .filter-form :deep(.el-input-number) {
    width: 100%;
  }
  .filter-form :deep(.el-form-item) {
    width: 100%;
  }
}
</style>
