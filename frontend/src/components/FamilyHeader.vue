<template>
  <header class="workspace-header">
    <div class="title-block">
      <p class="page-kicker">宗脉 · 修谱续牒</p>
      <h1>{{ title }}</h1>
    </div>
    <div class="workspace-actions">
      <el-select
        :model-value="familyId || undefined"
        placeholder="选择家族"
        aria-label="选择家族"
        class="family-select"
        :loading="loadingFamilies"
        @change="selectFamily"
      >
        <el-option
          v-for="item in families"
          :key="item.id"
          :label="item.name"
          :value="String(item.id)"
        />
      </el-select>
      <el-tag v-if="family" :type="canManage ? 'primary' : 'info'" effect="plain" disable-transitions>{{
        canManage ? "家族管理员" : "普通成员"
      }}</el-tag>
      <slot />
    </div>
  </header>
  <el-empty
    v-if="!loadingFamilies && !families.length"
    description="请从家族总览创建家族，或联系管理员关联账号"
  />
</template>

<script setup>
import { useFamilyContext } from "@/composables/useFamilyContext";
defineProps({ title: { type: String, required: true } });
const { families, loadingFamilies, familyId, family, canManage, selectFamily } =
  useFamilyContext();
</script>

<style scoped>
.title-block {
  min-width: 0;
}
.family-select {
  width: 230px;
}
@media (max-width: 900px) {
  .family-select {
    width: 100%;
  }
  .workspace-actions {
    width: 100%;
  }
}
</style>
