<template>
  <el-aside :width="isCollapse ? '80px' : '268px'" class="app-aside">
    <div class="brand" :class="{ 'brand--compact': isCollapse }">
      <el-image class="brand-logo" :src="brandLogo" alt="宗脉印章" fit="contain" />
      <div v-show="!isCollapse" class="info-card">
        <h1 class="brand-title">宗脉</h1>
        <p class="brand-subtitle">电子族谱 · 修谱续牒</p>
      </div>
    </div>

    <p v-show="!isCollapse" class="nav-caption">家 族 管 理</p>

    <el-menu
      :collapse="isCollapse"
      :default-active="route.path"
      class="sidebar-menu"
      @select="handleSelect"
    >
      <el-menu-item
        v-for="item in menuRoutes"
        :key="item.path"
        :index="'/back/' + item.path"
      >
        <el-icon><component :is="iconMap[item.meta.icon]" /></el-icon>
        <span class="nav-label">{{ item.meta.title }}</span>
      </el-menu-item>
    </el-menu>

    <div v-show="!isCollapse" class="aside-footer">
      <span class="footer-rule"></span>
      <p>一脉相承 · 敬宗收族</p>
      <small>宗脉 · 电子族谱管理系统</small>
    </div>
  </el-aside>
</template>
<script setup>
import { useRouter, useRoute } from 'vue-router'
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { PieChart, ChatLineSquare, Message, User, Picture, Setting } from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'
import brandLogo from '@/assets/brand-seal.svg'

const router = useRouter()
const route = useRoute()

const compactQuery = window.matchMedia('(max-width: 900px)')
const compact = ref(compactQuery.matches)
const updateCompact = event => { compact.value = event.matches }
onMounted(() => compactQuery.addEventListener('change', updateCompact))
onBeforeUnmount(() => compactQuery.removeEventListener('change', updateCompact))
const isCollapse = computed(() => useAdminStore().isCollapse || compact.value)

const iconMap = {
  PieChart,
  ChatLineSquare,
  Message,
  User,
  Picture,
  Setting,
}

const menuRoutes = router.options.routes.find(r => r.path === '/back')?.children || []

const handleSelect = (index) => {
  router.push({ path: index, query: route.query.familyId ? { familyId: route.query.familyId } : {} })
}
</script>

<style scoped>
.app-aside {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 0%, rgba(216, 189, 139, 0.14), transparent 18rem),
    radial-gradient(circle at 90% 100%, rgba(168, 71, 59, 0.12), transparent 16rem),
    linear-gradient(178deg, #223432 0%, #192826 58%, #14201f 100%);
  border-right: 1px solid rgba(216, 189, 139, 0.12);
  transition: width 0.18s ease-in-out;
  position: relative;
  z-index: 10;
}
.app-aside::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.35;
  background-image: repeating-linear-gradient(
    115deg,
    rgba(255, 255, 255, 0.018) 0 1px,
    transparent 1px 7px
  );
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 20px;
  min-height: 96px;
}
.brand--compact {
  justify-content: center;
  padding: 24px 0 20px;
}
.brand-logo {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.24);
}
.info-card {
  overflow: hidden;
}
.brand-title {
  margin: 0;
  font-family: var(--serif);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: #f5ead8;
  white-space: nowrap;
}
.brand-subtitle {
  margin: 4px 0 0;
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: rgba(226, 214, 190, 0.55);
  white-space: nowrap;
}
.nav-caption {
  margin: 2px 26px 10px;
  font-size: 10px;
  letter-spacing: 0.32em;
  color: rgba(216, 189, 139, 0.45);
}
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 0 10px;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(224, 219, 204, 0.72);
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-active-color: #f8ecda;
  --el-menu-item-height: 48px;
  overflow-y: auto;
  overflow-x: hidden;
}
.sidebar-menu:not(.el-menu--collapse) .el-menu-item {
  margin: 4px 8px;
  border-radius: 10px;
  letter-spacing: 0.06em;
}
.sidebar-menu .el-menu-item {
  position: relative;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}
.sidebar-menu .el-menu-item:hover {
  color: #f8ecda;
}
.sidebar-menu .el-menu-item.is-active {
  color: #f8ecda;
  background: linear-gradient(
    90deg,
    rgba(216, 189, 139, 0.16),
    rgba(216, 189, 139, 0.03)
  );
  box-shadow: inset 0 0 0 1px rgba(216, 189, 139, 0.1);
}
.sidebar-menu .el-menu-item.is-active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 13px;
  bottom: 13px;
  width: 3px;
  border-radius: 3px;
  background: linear-gradient(180deg, #d8bd8b, #a8473b);
}
.nav-label {
  font-size: 14px;
  white-space: nowrap;
}
.sidebar-menu :deep(.el-icon) {
  color: rgba(232, 226, 210, 0.78);
  transition: color 0.2s ease;
}
.sidebar-menu :deep(.el-menu-item:hover .el-icon),
.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #e3c99a;
}
.sidebar-menu:deep(.el-menu-tooltip__trigger) {
  justify-content: center;
}
.sidebar-menu.el-menu--collapse {
  width: 100%;
  padding: 0 8px;
}
.sidebar-menu.el-menu--collapse :deep(.el-menu-item) {
  width: 100%;
  padding: 0 !important;
  justify-content: center;
}
.aside-footer {
  margin-top: auto;
  padding: 20px 26px 26px;
  color: rgba(226, 214, 190, 0.4);
}
.footer-rule {
  display: block;
  width: 34px;
  height: 1px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #a8473b, rgba(168, 71, 59, 0));
}
.aside-footer p {
  margin: 0;
  font-family: var(--serif);
  font-size: 12.5px;
  letter-spacing: 0.16em;
  color: rgba(226, 214, 190, 0.6);
}
.aside-footer small {
  display: block;
  margin-top: 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: rgba(226, 214, 190, 0.28);
}
</style>
