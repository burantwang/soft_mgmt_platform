<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 品牌区 -->
      <div class="login-brand">
        <div class="brand-icon">
          <el-icon :size="42"><Platform /></el-icon>
        </div>
        <h1>研发业务平台</h1>
        <p>Sonic 版本发布 · Wiki 知识库 · 一体化研发工作台</p>
      </div>
      <!-- 表单区 -->
      <div class="login-form">
        <h2>欢迎登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ ...form })
    if (userStore.userInfo?.mustChangePwd === 1) {
      // 首次登录需强制修改初始密码
      router.push('/profile?forceChange=1')
      return
    }
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch {
    // 错误提示由 axios 拦截统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #2b5be3 0%, #1f3f9e 50%, #14256b 100%);
}

.login-card {
  display: flex;
  width: 820px;
  max-width: 92vw;
  min-height: 460px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.login-brand {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  background: linear-gradient(160deg, #2b5be3 0%, #1f3f9e 100%);
  color: #fff;

  h1 {
    margin: 8px 0 0;
    font-size: 24px;
    letter-spacing: 2px;
  }

  p {
    margin: 0;
    font-size: 13px;
    opacity: 0.85;
  }
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
}

.login-form {
  flex: 1;
  padding: 48px 44px;

  h2 {
    margin: 0 0 28px;
    font-size: 20px;
    color: var(--text-main);
  }
}

.login-btn {
  width: 100%;
  margin-top: 8px;
  letter-spacing: 4px;
}
</style>
