<template>
  <div class="navbar">
    <div class="navbar-left">
      <button class="collapse-btn" type="button" aria-label="切换侧栏" @click="handleCollapse">
        <el-icon :size="19">
          <Fold v-if="!adminStore.isCollapse" />
          <Expand v-else />
        </el-icon>
      </button>
      <div class="crumb">
        <span class="crumb-root">宗脉</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">{{ pageTitle }}</span>
      </div>
    </div>
    <div class="navbar-right">
      <span class="env-pill"><i class="env-dot"></i>家谱在线</span>
      <el-dropdown trigger="hover" @command="handleCommand">
        <div class="user-trigger">
          <el-avatar :size="34" class="user-avatar">{{ initial }}</el-avatar>
          <div class="user-meta">
            <strong>{{ adminStore.user?.username || '未登录' }}</strong>
            <small>{{ roleLabel }}</small>
          </div>
          <el-icon class="chev"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              <span class="dropdown-note">已登录用户</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Fold, Expand, SwitchButton, ArrowDown } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message-box/style/css'
import { useRouter, useRoute } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { logout } from '@/api/admin'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const pageTitle = computed(() => route.meta.title || '家族总览')
const initial = computed(() => (adminStore.user?.username || '宗').slice(0, 1).toUpperCase())
const roleLabel = computed(() =>
  adminStore.user?.systemAdmin ? '系统管理员' : '家族成员',
)

const handleCommand = async (command) => {
  if (command !== 'logout') return

  try {
    await ElMessageBox.confirm('确认要退出登录吗？', '退出登录', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await logout()
    adminStore.user = null
    await router.replace('/auth/login')
  } catch (error) {
    // 取消确认时结束操作，接口错误由请求层显示
  }
}

const handleCollapse = () => {
  adminStore.toggleCollapse()
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
  background: rgba(250, 246, 238, 0.97);
  border-bottom: 1px solid var(--line);
  position: relative;
}
.navbar::after {
  content: "";
  position: absolute;
  left: 24px;
  right: 24px;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(168, 71, 59, 0.24),
    rgba(173, 134, 80, 0.08) 28%,
    transparent 60%
  );
}
.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}
.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: rgba(255, 253, 248, 0.72);
  color: var(--ink-700);
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}
.collapse-btn:hover {
  color: var(--jade-700);
  border-color: rgba(50, 107, 96, 0.3);
  background: #fffdf8;
}
.crumb {
  display: flex;
  align-items: baseline;
  gap: 9px;
  min-width: 0;
  white-space: nowrap;
}
.crumb-root {
  font-family: var(--serif);
  font-size: 13px;
  letter-spacing: 0.14em;
  color: var(--bronze);
}
.crumb-sep {
  color: var(--line);
  font-size: 13px;
}
.crumb-current {
  font-family: var(--serif);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: 18px;
}
.env-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(50, 107, 96, 0.16);
  background: rgba(231, 240, 236, 0.6);
  color: var(--jade-700);
  font-size: 11.5px;
  letter-spacing: 0.08em;
}
.env-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4f8f6f;
  box-shadow: 0 0 0 3px rgba(79, 143, 111, 0.14);
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  padding: 5px 10px 5px 6px;
  border-radius: 12px;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s ease;
}
.user-trigger:hover {
  background: rgba(236, 228, 213, 0.55);
}
.user-avatar {
  background: linear-gradient(145deg, #326b60, #244f47);
  color: #f8ecda;
  font-family: var(--serif);
  font-size: 15px;
  letter-spacing: 0.04em;
  box-shadow: 0 4px 10px rgba(42, 93, 83, 0.22);
}
.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.user-meta strong {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-800);
  letter-spacing: 0.02em;
}
.user-meta small {
  font-size: 10.5px;
  color: var(--ink-400);
  letter-spacing: 0.04em;
}
.chev {
  color: var(--ink-400);
  font-size: 12px;
}
.dropdown-note {
  font-size: 12px;
  color: var(--ink-400);
  letter-spacing: 0.06em;
}
@media (max-width: 900px) {
  .navbar {
    padding: 0 14px;
  }
  .navbar-right {
    gap: 10px;
  }
  .env-pill,
  .user-meta,
  .chev {
    display: none;
  }
  .crumb-root,
  .crumb-sep {
    display: none;
  }
  .crumb-current {
    font-size: 16px;
  }
}
</style>
