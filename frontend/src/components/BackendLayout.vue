<template>
  <div class="common-layout">
    <el-container class="layout-container">
      <Sidebar />
      <el-container class="layout-body">
        <el-header class="layout-header">
          <Navbar />
        </el-header>
        <el-main class="main-content">
          <router-view class="content-container"></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
</script>

<style lang="scss" scoped>
.common-layout {
  height: 100vh;
  overflow: hidden;
  .layout-container {
    height: 100%;
  }
  .layout-body {
    min-width: 0;
    height: 100%;
  }
  .layout-header {
    height: 70px !important;
    padding: 0;
    flex-shrink: 0;
    position: relative;
    z-index: 9;
  }
  .main-content {
    height: calc(100vh - 70px);
    padding: 0;
    overflow-y: auto;
    overflow-x: hidden;
    background:
      radial-gradient(circle at 86% 6%, rgba(173, 134, 80, 0.08), transparent 24rem),
      radial-gradient(circle at 4% 92%, rgba(50, 107, 96, 0.06), transparent 26rem),
      transparent;
    .content-container {
      width: 100%;
      max-width: 1460px;
      margin: 0 auto;
      padding: 28px 32px 64px;
      background: transparent;
      min-height: calc(100vh - 70px);
    }
  }
}

/* Navbar 自带左右内边距和底部边框 */
.common-layout :deep(.el-header) {
  padding: 0;
}

@media (max-width: 900px) {
  .common-layout .main-content .content-container {
    padding: 18px 14px 44px;
  }
}
</style>
