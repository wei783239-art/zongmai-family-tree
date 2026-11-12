<template>
  <div class="auth-form">
    <router-link class="back-home" to="/public">
      <el-icon><Back /></el-icon>
      <span>返回公开首页</span>
    </router-link>

    <header class="auth-head">
      <p class="auth-eyebrow">宗脉 · 新建谱牒</p>
      <h2>创建账号</h2>
      <p class="auth-sub">建立您的修谱身份，加入或创建家族</p>
    </header>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="auth-form-body"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="formData.username"
          size="large"
          placeholder="3~32位英语字母、数字或下划线"
          :prefix-icon="User"
          autocomplete="username"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="formData.password"
          size="large"
          type="password"
          show-password
          placeholder="6~64个字符"
          :prefix-icon="Lock"
          autocomplete="new-password"
        />
      </el-form-item>

      <el-button
        class="submit-btn"
        type="primary"
        size="large"
        :loading="loading"
        @click="submitForm"
      >
        {{ loading ? "注册中..." : "注册" }}
      </el-button>
    </el-form>

    <p class="auth-switch">
      已有账号？
      <router-link to="/auth/login">返回登录</router-link>
    </p>
    <p class="auth-quote">以谱为脉 · 以字传家</p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Back, User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { register } from '@/api/admin'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const formData = reactive({
    username: '',
    password: ''
})

const rules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
    ]
}

const submitForm = async () => {
    if (!formRef.value || loading.value) return

    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    loading.value = true
    try {
        await register(formData)
        ElMessage.success('注册成功，请登录')
        await router.replace('/auth/login')

    } catch (error) {
        // 请求层统一显示后端返回的错误
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
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
