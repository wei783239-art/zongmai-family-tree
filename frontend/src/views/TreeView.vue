<template>
  <div>
    <FamilyHeader title="族谱关系图"
      ><el-button :loading="loading" @click="refresh++"
        >刷新图谱</el-button
      ></FamilyHeader
    >
    <el-alert
      v-if="failed"
      title="族谱图加载失败，请刷新重试"
      type="error"
      :closable="false"
    />
    <el-skeleton v-else-if="loading" :rows="6" animated />
    <template v-else-if="data">
      <section class="panel graph-panel">
        <div class="panel-head">
          <div>
            <p class="panel-title">世系总图</p>
            <p class="panel-sub">
              以人物关系绘制五代世系，点击卡片可查看完整档案与亲属
            </p>
          </div>
          <span class="panel-seal">世系</span>
        </div>
        <div class="workspace-actions graph-tools">
        <el-select
          v-model="rootId"
          clearable
          filterable
          placeholder="始祖或指定人物"
          style="width: 220px"
          ><el-option
            v-for="p in data.persons"
            :key="p.id"
            :label="`${p.name}（${p.id}）`"
            :value="String(p.id)"
        /></el-select>
        <el-select
          v-model="direction"
          aria-label="展开方向"
          style="width: 140px"
          ><el-option label="全家族" value="all" /><el-option
            label="查看后代"
            value="descendants" /><el-option label="查看祖先" value="ancestors"
        /></el-select>
        <el-select
          v-model="searchId"
          filterable
          clearable
          placeholder="搜索并定位人物"
          style="width: 220px"
          @change="locate"
          ><el-option
            v-for="p in visiblePeople"
            :key="p.id"
            :label="`${p.name}（${p.id}）`"
            :value="String(p.id)"
        /></el-select>
        <el-button :disabled="!searchId" @click="selectPerson(searchId)"
          >查看档案</el-button
        ><el-button @click="graphRef?.zoom(1.3)">放大</el-button>
        <el-button @click="graphRef?.zoom(1 / 1.3)">缩小</el-button>
        <el-button @click="graphRef?.fit()">适应画布</el-button
        ><el-button v-if="canManage" @click="exportImage">导出图片</el-button>
        <el-button v-if="collapsed.size" @click="collapsed = new Set()"
          >展开全部</el-button
        >
      </div>
        <div class="graph-legend">
          <span><i class="legend-line parent"></i>亲子关系</span>
          <span><i class="legend-line adoptive"></i>收养关系</span>
          <span><i class="legend-line spouse"></i>配偶关系</span>
          <span class="legend-tip">滚轮缩放 · 拖动画布或人物卡片</span>
        </div>
      <el-empty
        v-if="!visiblePeople.length"
        description="该范围暂无人物，请先录入成员或调整展开范围"
      />
        <FamilyTreeGraph
          v-else
          ref="graphRef"
          :key="graphKey"
          :persons="visiblePeople"
          :relations="visibleRelations"
          @select="selectPerson"
          @error="graphError"
        />
      </section>
      <div class="workspace-actions section-space">
        <el-button v-if="canManage" type="primary" @click="personEditor.open()"
          >新增成员</el-button
        >
        <RelationEditor
          :key="familyId"
          ref="relationEditor"
          :family-id="familyId"
          :can-manage="canManage"
          @changed="refresh++"
        />
      </div>
      <PersonEditor
        :key="familyId + '-editor'"
        ref="personEditor"
        :family-id="familyId"
        :can-manage="canManage"
        @saved="refresh++"
      />
    </template>
    <el-drawer v-model="drawer" title="人物档案" size="min(440px, 95vw)">
      <template v-if="selected">
        <el-image
          v-if="selected.avatar_url"
          :src="selected.avatar_url"
          class="person-avatar"
          fit="cover"
        />
        <h2>{{ selected.name }}</h2>
        <el-descriptions :column="1" border
          ><el-descriptions-item
            v-for="field in detailFields"
            :key="field[0]"
            :label="field[1]"
            >{{ selected[field[0]] ?? "未填写" }}</el-descriptions-item
          ></el-descriptions
        >
        <p class="biography">{{ selected.biography || "暂无人物简介" }}</p>
        <div
          v-for="group in relatives"
          :key="group.label"
          class="relative-group"
        >
          <strong>{{ group.label }}</strong>
          <div>
            <el-button
              v-for="p in group.people"
              :key="p.id"
              link
              type="primary"
              @click="selectPerson(String(p.id))"
              >{{ p.name }}（{{ p.id }}）</el-button
            ><span v-if="!group.people.length" class="muted">暂无记录</span>
          </div>
        </div>
        <div class="workspace-actions section-space">
          <el-button @click="editSelected">{{
            canManage ? "编辑资料" : "申请修改"
          }}</el-button
          ><el-button @click="addRelation">{{
            canManage ? "添加关系" : "申请关系"
          }}</el-button
          ><el-button @click="toggleCollapse">{{
            collapsed.has(String(selected.id)) ? "展开后代" : "折叠后代"
          }}</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>
<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import FamilyHeader from "@/components/FamilyHeader.vue";
import FamilyTreeGraph from "@/components/FamilyTreeGraph.vue";
import PersonEditor from "@/components/PersonEditor.vue";
import RelationEditor from "@/components/RelationEditor.vue";
import { useFamilyContext } from "@/composables/useFamilyContext";
import { familyRequest } from "@/api/workspace";
import { traceIds } from "@/utils/treeLayout";
const { familyId, canManage } = useFamilyContext();
const data = ref(null),
  loading = ref(false),
  failed = ref(false),
  refresh = ref(0);
const rootId = ref(""),
  direction = ref("all"),
  searchId = ref(""),
  collapsed = ref(new Set());
const graphRef = ref(null),
  personEditor = ref(null),
  relationEditor = ref(null),
  drawer = ref(false),
  selectedId = ref("");
watch(
  [familyId, refresh],
  async ([id], previous, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });
    data.value = null;
    failed.value = false;
    loading.value = Boolean(id);
    drawer.value = false;
    if (!id) return;
    try {
      const result = await familyRequest(id, "/tree");
      if (!cancelled) {
        data.value = result;
        if (previous?.[0] !== id) {
          rootId.value = result.family.ancestor_person_id
            ? String(result.family.ancestor_person_id)
            : "";
          direction.value = "all";
          collapsed.value = new Set();
        }
      }
    } catch {
      if (!cancelled) failed.value = true;
    } finally {
      if (!cancelled) loading.value = false;
    }
  },
  { immediate: true },
);
const visiblePeople = computed(() => {
  if (!data.value) return [];
  let ids = new Set(data.value.persons.map((p) => String(p.id)));
  if (rootId.value && direction.value !== "all") {
    ids = traceIds(data.value.relations, rootId.value, direction.value);
    ids.add(rootId.value);
    for (const r of data.value.relations.filter(
      (r) => r.relation_type === "SPOUSE",
    )) {
      if (ids.has(String(r.from_person_id))) ids.add(String(r.to_person_id));
      else if (ids.has(String(r.to_person_id)))
        ids.add(String(r.from_person_id));
    }
  }
  for (const id of collapsed.value)
    for (const child of traceIds(data.value.relations, id)) ids.delete(child);
  return data.value.persons.filter((p) => ids.has(String(p.id)));
});
const visibleRelations = computed(() => {
  const ids = new Set(visiblePeople.value.map((p) => String(p.id)));
  return (data.value?.relations || []).filter(
    (r) => ids.has(String(r.from_person_id)) && ids.has(String(r.to_person_id)),
  );
});
const graphKey = computed(
  () =>
    `${familyId.value}-${refresh.value}-${rootId.value}-${direction.value}-${[...collapsed.value].join(",")}`,
);
const selected = computed(() =>
  data.value?.persons.find((p) => String(p.id) === selectedId.value),
);
const detailFields = [
  ["id", "编号"],
  ["generation", "世代"],
  ["nickname", "字辈"],
  ["birth_date", "出生日期"],
  ["death_date", "逝世日期"],
  ["birth_place", "出生地"],
  ["hometown", "籍贯"],
];
const relatives = computed(() => {
  if (!selected.value) return [];
  const id = selectedId.value,
    edges = data.value.relations;
  const parentEdges = edges.filter((r) =>
    ["PARENT_CHILD", "ADOPTIVE_PARENT_CHILD"].includes(r.relation_type),
  );
  const parents = new Set(
    parentEdges
      .filter((r) => String(r.to_person_id) === id)
      .map((r) => String(r.from_person_id)),
  );
  const children = new Set(
    parentEdges
      .filter((r) => String(r.from_person_id) === id)
      .map((r) => String(r.to_person_id)),
  );
  const siblings = new Set(
    parentEdges
      .filter(
        (r) =>
          parents.has(String(r.from_person_id)) &&
          String(r.to_person_id) !== id,
      )
      .map((r) => String(r.to_person_id)),
  );
  const spouses = new Set(
    edges
      .filter(
        (r) =>
          r.relation_type === "SPOUSE" &&
          [String(r.from_person_id), String(r.to_person_id)].includes(id),
      )
      .map((r) =>
        String(r.from_person_id) === id
          ? String(r.to_person_id)
          : String(r.from_person_id),
      ),
  );
  return [
    { label: "父母（含收养）", ids: parents },
    { label: "配偶", ids: spouses },
    { label: "子女（含收养）", ids: children },
    { label: "兄弟姐妹（共享父母）", ids: siblings },
  ].map((group) => ({
    label: group.label,
    people: data.value.persons.filter((p) => group.ids.has(String(p.id))),
  }));
});
function selectPerson(id) {
  selectedId.value = id;
  drawer.value = true;
}
function locate(id) {
  if (id) graphRef.value?.focus(id).catch(graphError);
}
function editSelected() {
  drawer.value = false;
  personEditor.value.open(selected.value);
}
function addRelation() {
  drawer.value = false;
  relationEditor.value.open(selected.value.id);
}
function toggleCollapse() {
  const next = new Set(collapsed.value);
  const id = selectedId.value;
  if (next.has(id)) next.delete(id);
  else next.add(id);
  collapsed.value = next;
  drawer.value = false;
}
function graphError(error) {
  console.error(error);
  ElMessage.error(error.message || "图谱绘制失败，请刷新重试");
}
async function exportImage() {
  try {
    const url = await graphRef.value.image();
    const link = document.createElement("a");
    link.href = url;
    link.download = `family-${familyId.value}.png`;
    link.click();
  } catch (error) {
    graphError(error);
  }
}
</script>

<style scoped>
.graph-panel {
  padding-bottom: 18px;
}
.graph-tools {
  margin-bottom: 14px;
}
.graph-tools :deep(.el-select) {
  flex: 0 1 auto;
}
.graph-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 20px;
  margin: 2px 0 14px;
  padding: 10px 14px;
  border: 1px dashed var(--line);
  border-radius: 10px;
  background: rgba(250, 246, 238, 0.6);
  font-size: 12px;
  color: var(--ink-500);
}
.graph-legend span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}
.legend-line {
  display: inline-block;
  width: 22px;
  height: 0;
  border-top: 2px solid #3f6f63;
}
.legend-line.adoptive {
  border-top-style: dashed;
  border-top-color: #6b8f86;
}
.legend-line.spouse {
  border-top-style: dashed;
  border-top-color: #ad8650;
}
.legend-tip {
  margin-left: auto;
  color: var(--ink-400);
  letter-spacing: 0.04em;
}
@media (max-width: 700px) {
  .legend-tip {
    margin-left: 0;
  }
}
</style>

