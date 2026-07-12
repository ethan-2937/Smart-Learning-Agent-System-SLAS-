<script setup lang="ts">
import { ref } from 'vue'
import {
  ArrowRight,
  CircleCheckFilled,
  Connection,
  Files,
  Lock,
  Notebook,
  Reading,
  Setting,
  TrendCharts,
} from '@element-plus/icons-vue'

defineProps<{
  busy: boolean
  error: string
  logoUrl: string
}>()

const emit = defineEmits<{
  submit: [credentials: { username: string; password: string }]
}>()

const form = ref({ username: '', password: '' })

function fillDemoAccount(role: 'admin' | 'teacher' | 'student') {
  const accounts = {
    admin: { username: 'admin', password: 'admin123' },
    teacher: { username: 'teacher', password: 'teacher123' },
    student: { username: 'student', password: 'student123' },
  }
  form.value = accounts[role]
}

function submit() {
  emit('submit', { ...form.value })
}
</script>

<template>
  <section class="login-shell">
    <div class="login-grid-pattern" aria-hidden="true"></div>
    <div class="login-stage">
      <aside class="login-story">
        <div class="login-brand">
          <span class="brand-mark"><img :src="logoUrl" alt="SLAS 标志" /></span>
          <span>
            <strong>SLAS 智学系统</strong>
            <small>Smart Learning Agent System</small>
          </span>
        </div>

        <div class="login-story__copy">
          <span class="section-kicker">AI LEARNING WORKSPACE</span>
          <h1>让每一份教材，<br />都有清晰的学习路径。</h1>
          <p>连接教材知识、智能出题与练习反馈，为不同学科提供可追溯的学习闭环。</p>
        </div>

        <div class="login-flow" aria-label="系统学习流程">
          <div class="login-flow__item">
            <span><el-icon><Files /></el-icon></span>
            <strong>教材入库</strong>
            <small>解析与切片</small>
          </div>
          <el-icon class="login-flow__arrow"><ArrowRight /></el-icon>
          <div class="login-flow__item">
            <span><el-icon><Connection /></el-icon></span>
            <strong>知识检索</strong>
            <small>向量证据</small>
          </div>
          <el-icon class="login-flow__arrow"><ArrowRight /></el-icon>
          <div class="login-flow__item">
            <span><el-icon><TrendCharts /></el-icon></span>
            <strong>反馈提升</strong>
            <small>练习画像</small>
          </div>
        </div>

        <p class="login-story__foot"><CircleCheckFilled /> 支持英语、计算机、通识课与自定义课程</p>
      </aside>

      <div class="login-panel">
        <div class="login-panel__head">
          <span class="login-panel__badge"><Lock /> 安全登录</span>
          <h2>欢迎回来</h2>
          <p>请选择演示角色，或使用你的系统账号登录。</p>
        </div>

        <div class="demo-accounts" aria-label="演示账号快捷选择">
          <button type="button" @click="fillDemoAccount('student')">
            <el-icon><Reading /></el-icon>
            <span><strong>学生</strong><small>学习与复习</small></span>
          </button>
          <button type="button" @click="fillDemoAccount('teacher')">
            <el-icon><Notebook /></el-icon>
            <span><strong>教师</strong><small>内容与题库</small></span>
          </button>
          <button type="button" @click="fillDemoAccount('admin')">
            <el-icon><Setting /></el-icon>
            <span><strong>管理员</strong><small>系统与权限</small></span>
          </button>
        </div>

        <div class="login-divider"><span>账号登录</span></div>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
        <div class="login-form">
          <label>
            <span>用户名</span>
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" autocomplete="username" @keyup.enter="submit" />
          </label>
          <label>
            <span>密码</span>
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" autocomplete="current-password" @keyup.enter="submit" />
          </label>
          <el-button type="primary" size="large" :loading="busy" @click="submit">
            进入学习空间
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <p class="login-panel__foot"><Lock /> 登录状态由 JWT 安全校验</p>
      </div>
    </div>
  </section>
</template>
