<template>
  <div>
    <FamilyHeader title="家族相册与文献">
      <el-button v-if="canManage" type="primary" @click="openUpload"
        >上传档案</el-button
      >
      <el-button :loading="loading" @click="reload++">刷新</el-button>
    </FamilyHeader>

    <section class="panel album-panel">
      <div class="panel-head">
        <div>
          <p class="panel-title">影像与文献</p>
          <p class="panel-sub">
            收录家族照片、手稿与 PDF 文献，可按标题检索
          </p>
        </div>
        <span class="panel-seal">藏</span>
      </div>

      <div v-if="familyId" class="album-toolbar">
        <el-input
          v-model="keyword"
          placeholder="按标题搜索档案"
          clearable
          class="album-search"
        />
        <span class="album-count">共 {{ filtered.length }} 件档案</span>
      </div>

      <el-skeleton v-if="loading" :rows="5" animated />
      <el-alert
        v-else-if="failed"
        title="档案加载失败，请刷新重试"
        type="error"
        :closable="false"
      />
      <el-empty
        v-else-if="familyId && !filtered.length"
        description="暂无匹配的照片或文献"
      />
      <div v-else class="media-grid">
        <article v-for="item in filtered" :key="item.id" class="media-item">
          <el-image
            v-if="item.content_type.startsWith('image/')"
            :src="fileUrl(item)"
            :preview-src-list="[fileUrl(item)]"
            fit="cover"
            loading="lazy"
          />
          <a v-else :href="fileUrl(item)" class="document-preview" download
            >下载 PDF 文献</a
          >
          <div class="media-caption">
            <h3>{{ item.title }}</h3>
            <p class="muted file-name">{{ item.file_name }}</p>
            <p v-if="item.person_id">
              人物 ·
              {{
                people.find((p) => p.id === item.person_id)?.name ||
                item.person_id
              }}
            </p>
            <p v-if="item.event_id">
              事件 ·
              {{
                events.find((e) => e.id === item.event_id)?.title || item.event_id
              }}
            </p>
            <div class="media-footer">
              <el-tag v-if="item.visibility === 'PRIVATE'" type="info" effect="plain" disable-transitions
                >仅管理员</el-tag
              >
              <span v-else class="media-visible">家族可见</span>
              <el-button
                v-if="canManage"
                link
                type="danger"
                :disabled="saving"
                @click="remove(item)"
                >删除</el-button
              >
            </div>
          </div>
        </article>
      </div>
    </section>

    <el-dialog
      v-model="dialog"
      title="上传照片或文献"
      width="min(560px, 94vw)"
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <el-form label-position="top" :disabled="saving">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="160" />
        </el-form-item>
        <el-form-item label="文件（PNG、JPEG、GIF 或 PDF，最大 8MB）" required>
          <input
            ref="fileInput"
            type="file"
            class="file-input"
            accept="image/png,image/jpeg,image/gif,application/pdf"
            :disabled="saving"
            @change="chooseFile"
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="关联人物">
            <el-select v-model="form.personId" clearable filterable>
              <el-option
                v-for="p in people"
                :key="p.id"
                :label="p.name + '（' + p.id + '）'"
                :value="p.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关联事件">
            <el-select v-model="form.eventId" clearable filterable>
              <el-option
                v-for="e in events"
                :key="e.id"
                :label="e.title"
                :value="e.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="可见范围">
          <el-select v-model="form.visibility">
            <el-option label="家族可见" value="FAMILY" />
            <el-option label="仅管理员" value="PRIVATE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="upload"
          >上传</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import FamilyHeader from "@/components/FamilyHeader.vue";
import { useFamilyContext } from "@/composables/useFamilyContext";
import { familyRequest } from "@/api/workspace";

const { familyId, canManage } = useFamilyContext();
const media = ref([]),
  people = ref([]),
  events = ref([]),
  loading = ref(false),
  failed = ref(false),
  reload = ref(0),
  keyword = ref(""),
  saving = ref(false),
  dialog = ref(false),
  fileInput = ref(null),
  file = ref(null);
const form = reactive({
  title: "",
  personId: null,
  eventId: null,
  visibility: "FAMILY",
});
const filtered = computed(() =>
  media.value.filter((item) => item.title.includes(keyword.value.trim())),
);
const fileUrl = (item) =>
  "/api/families/" + familyId.value + "/media/" + item.id + "/file";
watch(
  [familyId, reload],
  async ([id], previous, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });
    media.value = [];
    loading.value = Boolean(id);
    failed.value = false;
    dialog.value = false;
    if (!id) return;
    try {
      const result = await Promise.all([
        familyRequest(id, "/media"),
        familyRequest(id, "/persons"),
        familyRequest(id, "/events"),
      ]);
      if (!cancelled) [media.value, people.value, events.value] = result;
    } catch {
      if (!cancelled) failed.value = true;
    } finally {
      if (!cancelled) loading.value = false;
    }
  },
  { immediate: true },
);
async function openUpload() {
  Object.assign(form, {
    title: "",
    personId: null,
    eventId: null,
    visibility: "FAMILY",
  });
  file.value = null;
  dialog.value = true;
  await nextTick();
  if (fileInput.value) fileInput.value.value = "";
}
function chooseFile(event) {
  file.value = event.target.files[0] || null;
  if (file.value && !form.title) form.title = file.value.name.slice(0, 160);
}
async function upload() {
  if (saving.value) return;
  if (!file.value || !form.title.trim())
    return ElMessage.warning("请选择文件并填写标题");
  if (file.value.size > 8 * 1024 * 1024)
    return ElMessage.warning("文件不能超过8MB");
  const body = new FormData();
  body.append("file", file.value);
  body.append("title", form.title.trim());
  body.append("visibility", form.visibility);
  if (form.personId) body.append("personId", form.personId);
  if (form.eventId) body.append("eventId", form.eventId);
  saving.value = true;
  try {
    await familyRequest(familyId.value, "/media", {
      method: "post",
      data: body,
      timeout: 60000,
    });
    dialog.value = false;
    reload.value++;
    ElMessage.success("档案已上传");
  } catch {
    /* 全局提示 */
  } finally {
    saving.value = false;
  }
}
async function remove(item) {
  try {
    await ElMessageBox.confirm(
      "删除“" + item.title + "”？使用此图片的头像也会清除。",
      "删除档案",
      { type: "warning" },
    );
    saving.value = true;
    await familyRequest(familyId.value, "/media/" + item.id, {
      method: "delete",
    });
    reload.value++;
  } catch {
    /* 取消或全局提示 */
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.album-panel {
  min-height: 280px;
}
.album-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed var(--line);
}
.album-search {
  max-width: 320px;
}
.album-count {
  font-size: 12px;
  letter-spacing: 0.12em;
  color: var(--ink-400);
}
.media-caption h3 {
  font-family: var(--serif);
  letter-spacing: 0.05em;
}
.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.media-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--line-soft);
}
.media-visible {
  font-size: 11.5px;
  color: var(--ink-400);
  letter-spacing: 0.08em;
}
.file-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px dashed var(--line);
  border-radius: 9px;
  background: #fbf7ee;
  color: var(--ink-500);
  font-size: 12.5px;
}
</style>
