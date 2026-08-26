<template>
  <div>
    <el-alert v-if="forceChange" type="warning" :closable="false" show-icon class="mb-16">
      <template #title>出于安全考虑，首次登录必须修改初始密码后才能使用系统</template>
    </el-alert>

    <el-row :gutter="16">
      <!-- 个人信息 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">个人信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="账号">{{ userStore.userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag v-for="r in userStore.userInfo?.roleNames || []" :key="r" size="small" class="mr-4">{{ r }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ userStore.userInfo?.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 修改密码 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">修改密码</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="pwd-form" @keyup.enter="onSubmit">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password placeholder="不少于 6 位" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="onSubmit">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { changePasswordApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const forceChange = computed(() => route.query.forceChange === '1' || userStore.userInfo?.mustChangePwd === 1)

async function onSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await changePasswordApi({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    // 刷新用户信息(清除强制改密标记)
    await userStore.fetchInfo()
    if (forceChange.value) {
      ElMessage.success('欢迎使用研发业务平台')
      router.push('/')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.mr-4 {
  margin-right: 4px;
}

.card-title {
  font-weight: 600;
}

.pwd-form {
  max-width: 480px;
}
</style>
