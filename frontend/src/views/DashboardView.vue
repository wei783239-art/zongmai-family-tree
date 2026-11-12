<template>
  <div>
    <FamilyHeader title="家族总览">
      <el-button :loading="busy" @click="makeDemo">创建五代演示家族</el-button>
      <el-button type="primary" @click="openEditor()">创建家族</el-button>
      <el-button :loading="loadingFamilies" @click="refreshFamilies">刷新</el-button>
    </FamilyHeader>

    <section v-if="family" class="family-hero">
      <span class="hero-watermark" aria-hidden="true">谱</span>
      <div class="hero-copy">
        <p class="hero-eyebrow">
          <span class="eyebrow-rule"></span>
          世家档案 · {{ family.surname ? family.surname + '氏' : '宗族' }}
        </p>
        <h2>{{ family.name }}</h2>
        <div class="hero-tags">
          <span v-if="family.ancestral_hall">堂号 · {{ family.ancestral_hall }}</span>
          <span v-if="family.hometown">祖籍 · {{ family.hometown }}</span>
          <span v-if="family.published" class="hero-public">公开简介已开启</span>
        </div>
        <blockquote v-if="family.motto" class="hero-motto">
          {{ family.motto }}
          <cite>家训</cite>
        </blockquote>
        <p class="hero-desc">{{ family.description || "暂无家族简介" }}</p>
      </div>

      <div v-if="stats" class="hero-stats">
        <div v-for="card in statCards" :key="card.label" class="hero-stat">
          <strong>{{ card.value }}</strong>
          <span>{{ card.label }}</span>
        </div>
      </div>
      <div v-else-if="!statsFailed" class="hero-stats hero-stats--loading">
        <el-skeleton :rows="2" animated />
      </div>
    </section>

    <el-alert
      v-if="statsFailed"
      class="stats-alert"
      title="统计加载失败，请切换家族或刷新重试"
      type="error"
      :closable="false"
    />

    <section class="panel family-list-panel">
      <div class="panel-head">
        <div>
          <p class="panel-title">家族名录</p>
          <p class="panel-sub">
            当前账号关联 {{ families.length }} 个家族，可进入族谱、成员档案或维护家族资料
          </p>
        </div>
        <span class="panel-seal">世系</span>
      </div>

      <el-table :data="families" row-key="id" border class="family-table">
        <el-table-column prop="name" label="家族名称" min-width="180" />
        <el-table-column prop="surname" label="姓氏" width="80" />
        <el-table-column prop="ancestral_hall" label="堂号" width="130" />
        <el-table-column prop="hometown" label="祖籍" min-width="130" />
        <el-table-column prop="person_count" label="可见人数" width="100" />
        <el-table-column label="身份" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.role === 'FAMILY_ADMIN' ? 'primary' : 'info'"
              effect="plain"
              size="small"
              disable-transitions
            >
              {{ row.role === "FAMILY_ADMIN" ? "管理员" : "成员" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="250">
          <template #default="{ row }">
            <el-button link type="primary" @click="enter(row, 'knowledge')"
              >查看族谱</el-button
            >
            <el-button link type="primary" @click="enter(row, 'consultations')"
              >成员档案</el-button
            >
            <el-button
              v-if="row.role === 'FAMILY_ADMIN'"
              link
              @click="openEditor(row)"
              >编辑</el-button
            >
            <el-button
              v-if="row.role === 'FAMILY_ADMIN'"
              link
              type="danger"
              :disabled="busy"
              @click="remove(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="stats && stats.generations.length" class="panel generation-panel">
      <div class="panel-head">
        <div>
          <p class="panel-title">世代分布</p>
          <p class="panel-sub">
            共 {{ stats.generations.length }} 个世代，{{ stats.total }} 份家族档案
          </p>
        </div>
        <span class="panel-seal">昭穆</span>
      </div>
      <div class="generation-bars">
        <div
          v-for="row in stats.generations"
          :key="row.generation || 'unknown'"
          class="generation-row"
        >
          <span class="generation-name">{{
            row.generation ? "第 " + row.generation + " 代" : "未填写"
          }}</span>
          <div class="generation-track">
            <span :style="{ width: barWidth(row.count) }"></span>
          </div>
          <strong>{{ row.count }}</strong>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="dialog"
      :title="editing ? '编辑家族' : '创建家族'"
      width="min(580px, 94vw)"
      :close-on-click-modal="false"
      :show-close="!busy"
      :close-on-press-escape="!busy"
    >
      <el-form :model="form" label-position="top" :disabled="busy">
        <el-form-item
          v-for="field in fields"
          :key="field.key"
          :label="field.label"
          :required="field.key === 'name'"
        >
          <el-input
            v-model="form[field.key]"
            :type="field.multiline ? 'textarea' : 'text'"
            :maxlength="field.max"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="公开家族简介">
          <el-switch v-model="form.published" />
          <span class="muted" style="margin-left: 12px"
            >公开后游客可见家族名称、姓氏和简介</span
          >
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="busy" @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import FamilyHeader from "@/components/FamilyHeader.vue";
import { useFamilyContext } from "@/composables/useFamilyContext";
import { createFamily, updateFamliy, deleteFamily } from "@/api/family";
import { createDemoFamily, familyRequest } from "@/api/workspace";

const router = useRouter();
const { familyId, family, families, loadingFamilies, refreshFamilies } =
  useFamilyContext();
const busy = ref(false),
  dialog = ref(false),
  editing = ref(null),
  stats = ref(null),
  statsFailed = ref(false);

const fields = [
  { key: "name", label: "家族名称", max: 120 },
  { key: "surname", label: "姓氏", max: 64 },
  { key: "ancestral_hall", label: "堂号", max: 120 },
  { key: "hometown", label: "祖籍", max: 160 },
  { key: "motto", label: "家训", max: 2000, multiline: true },
  { key: "description", label: "家族简介", max: 20000, multiline: true },
];
const form = reactive({
  ...Object.fromEntries(fields.map((f) => [f.key, ""])),
  published: false,
});

const statCards = computed(() => {
  if (!stats.value) return [];
  return [
    { label: "档案人数", value: stats.value.total },
    { label: "未记载逝世日期", value: stats.value.withoutDeathDate },
    { label: "亲属关系", value: stats.value.relations },
    { label: "家族事件", value: stats.value.events },
  ];
});
const maxGeneration = computed(() =>
  Math.max(1, ...(stats.value?.generations || []).map((row) => row.count)),
);
const barWidth = (count) =>
  Math.max(6, Math.round((count / maxGeneration.value) * 100)) + "%";

function openEditor(row = null) {
  editing.value = row;
  for (const field of fields) form[field.key] = row?.[field.key] || "";
  form.published = Boolean(row?.published);
  dialog.value = true;
}
function enter(row, page) {
  router.push({ path: "/back/" + page, query: { familyId: String(row.id) } });
}
async function save() {
  if (busy.value) return;
  if (!form.name.trim()) return ElMessage.warning("请输入家族名称");
  busy.value = true;
  try {
    if (editing.value) await updateFamliy(editing.value.id, { ...form });
    else await createFamily({ ...form });
    dialog.value = false;
    await refreshFamilies();
    ElMessage.success("家族已保存");
  } catch {
    /* 全局提示 */
  } finally {
    busy.value = false;
  }
}
async function remove(row) {
  if (busy.value) return;
  try {
    await ElMessageBox.confirm(
      "删除家族“" + row.name + "”？有关联档案的家族须先处理关联数据。",
      "删除家族",
      { type: "warning" },
    );
    busy.value = true;
    await deleteFamily(row.id);
    await refreshFamilies();
    if (String(row.id) === familyId.value) await router.replace({ query: {} });
    ElMessage.success("家族已删除");
  } catch {
    /* 取消或全局提示 */
  } finally {
    busy.value = false;
  }
}
async function makeDemo() {
  if (busy.value) return;
  busy.value = true;
  try {
    const result = await createDemoFamily();
    await refreshFamilies();
    router.push({
      path: "/back/knowledge",
      query: { familyId: String(result.id) },
    });
  } catch {
    /* 全局提示 */
  } finally {
    busy.value = false;
  }
}
watch(
  [familyId, families],
  async ([id], previous, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });
    stats.value = null;
    statsFailed.value = false;
    if (!id) return;
    try {
      const result = await familyRequest(id, "/stats");
      if (!cancelled) stats.value = result;
    } catch {
      if (!cancelled) statsFailed.value = true;
    }
  },
  { immediate: true },
);
</script>

<style scoped>
.family-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 34px;
  align-items: center;
  padding: 38px 42px;
  border-radius: 20px;
  overflow: hidden;
  color: #f3ead9;
  background:
    radial-gradient(circle at 88% 12%, rgba(216, 189, 139, 0.16), transparent 22rem),
    radial-gradient(circle at 0% 100%, rgba(168, 71, 59, 0.18), transparent 26rem),
    linear-gradient(135deg, #243c38 0%, #1a2b28 58%, #152320 100%);
  box-shadow: 0 22px 50px rgba(26, 43, 40, 0.22);
  border: 1px solid rgba(216, 189, 139, 0.16);
}
.family-hero::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.5;
  background-image: repeating-linear-gradient(
    118deg,
    rgba(255, 255, 255, 0.022) 0 1px,
    transparent 1px 6px
  );
}
.hero-watermark {
  position: absolute;
  right: -8px;
  bottom: -58px;
  font-family: var(--serif);
  font-size: 240px;
  line-height: 1;
  color: rgba(243, 234, 217, 0.045);
  pointer-events: none;
  user-select: none;
}
.hero-copy {
  position: relative;
  z-index: 1;
  min-width: 0;
}
.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 10px;
  font-size: 11px;
  letter-spacing: 0.28em;
  color: rgba(216, 189, 139, 0.8);
}
.eyebrow-rule {
  width: 26px;
  height: 1px;
  background: rgba(216, 189, 139, 0.6);
}
.hero-copy h2 {
  margin: 0;
  font-size: clamp(30px, 3.2vw, 42px);
  letter-spacing: 0.12em;
  color: #f7eedf;
  font-weight: 600;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.18);
}
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  margin-top: 14px;
}
.hero-tags span {
  padding: 4px 11px;
  border-radius: 999px;
  border: 1px solid rgba(216, 189, 139, 0.24);
  background: rgba(255, 255, 255, 0.045);
  color: rgba(243, 234, 217, 0.78);
  font-size: 11.5px;
  letter-spacing: 0.08em;
}
.hero-tags .hero-public {
  border-color: rgba(168, 71, 59, 0.45);
  color: #e7b9a8;
}
.hero-motto {
  position: relative;
  margin: 22px 0 0;
  padding-left: 16px;
  font-family: var(--serif);
  font-size: 16px;
  line-height: 1.8;
  letter-spacing: 0.08em;
  color: #e9d8b9;
  border-left: 2px solid rgba(168, 71, 59, 0.7);
}
.hero-motto cite {
  display: block;
  margin-top: 4px;
  font-family: var(--sans);
  font-style: normal;
  font-size: 10.5px;
  letter-spacing: 0.3em;
  color: rgba(216, 189, 139, 0.55);
}
.hero-desc {
  margin: 16px 0 0;
  max-width: 62ch;
  font-size: 13.5px;
  line-height: 1.9;
  color: rgba(233, 225, 209, 0.68);
}
.hero-stats {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.hero-stat {
  padding: 18px 18px 16px;
  border-radius: 14px;
  border: 1px solid rgba(216, 189, 139, 0.18);
  background: rgba(255, 255, 255, 0.055);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.hero-stat strong {
  font-family: var(--serif);
  font-size: 32px;
  font-weight: 500;
  line-height: 1;
  color: #f4e7d0;
}
.hero-stat span {
  font-size: 11px;
  letter-spacing: 0.12em;
  color: rgba(226, 214, 190, 0.6);
}
.hero-stats--loading {
  display: block;
}
.stats-alert {
  margin-top: 18px;
  border-radius: 12px;
}
.family-list-panel {
  margin-top: 20px;
}
.generation-panel {
  margin-top: 20px;
}
.generation-bars {
  display: grid;
  gap: 12px;
}
.generation-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 14px;
}
.generation-name {
  font-family: var(--serif);
  font-size: 13.5px;
  letter-spacing: 0.06em;
  color: var(--ink-700);
}
.generation-track {
  height: 9px;
  border-radius: 999px;
  background: #f0e9db;
  overflow: hidden;
}
.generation-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #3b7669, #6b9b8e);
  box-shadow: 0 0 12px rgba(59, 118, 105, 0.2);
}
.generation-row strong {
  font-family: var(--serif);
  font-size: 16px;
  color: var(--jade-700);
  text-align: right;
}
@media (max-width: 1080px) {
  .family-hero {
    grid-template-columns: 1fr;
    gap: 24px;
    padding: 30px;
  }
  .hero-stats {
    grid-template-columns: repeat(4, 1fr);
  }
}
@media (max-width: 700px) {
  .family-hero {
    padding: 26px 20px;
  }
  .hero-stats {
    grid-template-columns: 1fr 1fr;
  }
  .generation-row {
    grid-template-columns: 72px minmax(0, 1fr) 34px;
  }
}
</style>
