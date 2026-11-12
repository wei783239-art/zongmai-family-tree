<template>
  <div class="auth-form">
    <router-link class="back-home" to="/public">
      <el-icon><Back /></el-icon>
      <span>返回公开首页</span>
    </router-link>

    <header class="auth-head">
      <p class="auth-eyebrow">宗脉 · 修谱续牒</p>
      <h2>欢迎回来</h2>
      <p class="auth-sub">登录后继续整理您的家族档案</p>
    </header>

    <el-form
      ref="ruleFormRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="auth-form-body"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="formData.username"
          size="large"
          placeholder="请输入用户名"
          :prefix-icon="User"
          autocomplete="username"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="formData.password"
          size="large"
          type="password"
          placeholder="请输入密码"
          :prefix-icon="Lock"
          show-password
          autocomplete="current-password"
        />
      </el-form-item>
      <el-button
        class="submit-btn"
        size="large"
        type="primary"
        :loading="loading"
        @click="submitForm"
      >
        {{ loading ? "登录中..." : "登录" }}
      </el-button>
    </el-form>

    <p class="auth-switch">
      还没有账号？
      <router-link to="/auth/register">创建账号</router-link>
    </p>
    <p class="auth-quote">一脉相承 · 慎终追远</p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Back, User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import { login } from '@/api/admin'

const router = useRouter()
const ruleFormRef = ref()
const loading = ref(false)
const formData = reactive({
    username: '',
    password: ''
})
const rules = reactive({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
    ]
})
const submitForm = async () => {
    if (!ruleFormRef.value || loading.value) return
    const valid = await ruleFormRef.value.validate().catch(() => false)
    if (!valid) return

    loading.value = true
    try {
        await login(formData)
        ElMessage.success('登录成功')
        await router.push('/back/dashboard')
    } catch (error) {
        // 请求拦截器会显示后端返回的错误信息
    } finally {
        loading.value = false
    }
}
</script>

<style scoped lang="scss">
.auth-form {
  width: 100%;
}
.back-home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 30px;
  font-size: 12.5px;
  letter-spacing: 0.04em;
  color: var(--ink-400);
}
.back-home:hover {
  color: var(--cinnabar);
}
.auth-head {
  margin-bottom: 28px;
}
.auth-eyebrow {
  margin: 0 0 8px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--bronze);
}
.auth-head h2 {
  margin: 0;
  font-family: var(--serif);
  font-size: 32px;
  letter-spacing: 0.08em;
  color: var(--ink-900);
}
.auth-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--ink-400);
  letter-spacing: 0.02em;
}
.auth-form-body :deep(.el-form-item__label) {
  font-size: 12.5px;
  letter-spacing: 0.08em;
}
.auth-form-body :deep(.el-input__wrapper) {
  height: 46px;
  background: #fbf7ee;
}
.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 12px;
  font-size: 15px;
  letter-spacing: 0.18em;
}
.auth-switch {
  margin: 24px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--ink-400);
}
.auth-switch a {
  color: var(--jade-600);
  font-weight: 600;
}
.auth-quote {
  margin: 22px 0 0;
  text-align: center;
  font-family: var(--serif);
  font-size: 11px;
  letter-spacing: 0.24em;
  color: rgba(139, 154, 148, 0.7);
}
</style>
