<template>
  <div>
    <FamilyHeader title="家族大事记">
      <el-button v-if="canManage" type="primary" @click="edit()"
        >发布事件</el-button
      >
      <el-button :loading="loading" @click="reload++">刷新</el-button>
    </FamilyHeader>

    <section class="panel events-panel">
      <div class="panel-head">
        <div>
          <p class="panel-title">家族纪事</p>
          <p class="panel-sub">记录始迁、修谱、婚丧与家族重要时刻</p>
        </div>
        <span class="panel-seal">纪事</span>
      </div>

      <el-skeleton v-if="loading" :rows="5" animated />
      <el-alert
        v-else-if="failed"
        title="事件加载失败，请刷新重试"
        type="error"
        :closable="false"
      />
      <el-empty
        v-else-if="familyId && !events.length"
        description="暂无家族事件，可从右上角发布第一条纪事"
      />
      <el-timeline v-else class="event-timeline">
        <el-timeline-item
          v-for="event in events"
          :key="event.id"
          :timestamp="event.event_date || '日期未详'"
          placement="top"
        >
          <article class="event-card">
            <header class="event-card-head">
              <h2>{{ event.title }}</h2>
              <span class="event-seal" aria-hidden="true">记</span>
            </header>
            <p class="event-meta">
              {{ event.location || "地点未详"
              }}<span v-if="event.person_name"> · {{ event.person_name }}</span>
            </p>
            <p class="biography">{{ event.content }}</p>
            <div v-if="canManage" class="event-actions">
              <el-button link type="primary" @click="edit(event)"
                >编辑</el-button
              >
              <el-button
                link
                type="danger"
                :disabled="saving"
                @click="remove(event)"
                >删除</el-button
              >
            </div>
          </article>
        </el-timeline-item>
      </el-timeline>
    </section>

    <el-dialog
      v-model="dialog"
      :title="editing ? '编辑事件' : '发布事件'"
      width="min(600px, 94vw)"
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <el-form label-position="top" :disabled="saving">
        <el-form-item label="事件标题" required>
          <el-input v-model="form.title" maxlength="160" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="日期">
            <el-date-picker
              v-model="form.event_date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
          <el-form-item label="地点">
            <el-input v-model="form.location" maxlength="160" />
          </el-form-item>
        </div>
        <el-form-item label="关联人物">
          <el-select v-model="form.person_id" clearable filterable>
            <el-option
              v-for="p in people"
              :key="p.id"
              :value="p.id"
              :label="p.name + '（' + p.id + '）'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="事件内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="20000"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save"
          >保存</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import FamilyHeader from "@/components/FamilyHeader.vue";
import { useFamilyContext } from "@/composables/useFamilyContext";
import { familyRequest } from "@/api/workspace";

const { familyId, canManage } = useFamilyContext();
const events = ref([]),
  people = ref([]),
  loading = ref(false),
  failed = ref(false),
  reload = ref(0),
  saving = ref(false),
  dialog = ref(false),
  editing = ref(null);
const blank = () => ({
  title: "",
  event_date: null,
  location: "",
  content: "",
  person_id: null,
});
const form = reactive(blank());
watch(
  [familyId, reload],
  async ([id], previous, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });
    events.value = [];
    loading.value = Boolean(id);
    failed.value = false;
    dialog.value = false;
    if (!id) return;
    try {
      const result = await Promise.all([
        familyRequest(id, "/events"),
        familyRequest(id, "/persons"),
      ]);
      if (!cancelled) [events.value, people.value] = result;
    } catch {
      if (!cancelled) failed.value = true;
    } finally {
      if (!cancelled) loading.value = false;
    }
  },
  { immediate: true },
);
function edit(event = null) {
  editing.value = event;
  Object.assign(form, blank(), event || {});
  dialog.value = true;
}
async function save() {
  if (saving.value) return;
  if (!form.title.trim()) return ElMessage.warning("请输入事件标题");
  saving.value = true;
  try {
    await familyRequest(
      familyId.value,
      editing.value ? "/events/" + editing.value.id : "/events",
      {
        method: editing.value ? "put" : "post",
        data: {
          title: form.title.trim(),
          event_date: form.event_date || null,
          location: form.location,
          content: form.content,
          person_id: form.person_id || null,
        },
      },
    );
    dialog.value = false;
    reload.value++;
    ElMessage.success("事件已保存");
  } catch {
    /* 全局提示 */
  } finally {
    saving.value = false;
  }
}
async function remove(event) {
  try {
    await ElMessageBox.confirm("删除事件“" + event.title + "”？", "删除事件", {
      type: "warning",
    });
    saving.value = true;
    await familyRequest(familyId.value, "/events/" + event.id, {
      method: "delete",
    });
    reload.value++;
  } catch {
    /* 全局提示 */
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.events-panel {
  padding-bottom: 10px;
}
.event-timeline {
  margin-top: 10px;
}
.event-card {
  position: relative;
  padding: 20px 24px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: linear-gradient(180deg, #fffdf8, #fdf9f0);
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.event-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.event-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.event-card-head h2 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0.06em;
}
.event-seal {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(168, 71, 59, 0.5);
  border-radius: 5px;
  color: var(--cinnabar);
  font-family: var(--serif);
  font-size: 13px;
  opacity: 0.72;
  transform: rotate(-4deg);
}
.event-meta {
  margin: 8px 0 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--bronze);
}
.event-card .biography {
  margin: 12px 0 0;
  color: var(--ink-700);
  font-size: 13.5px;
}
.event-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--line-soft);
}
</style>
