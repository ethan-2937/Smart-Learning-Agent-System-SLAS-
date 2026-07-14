<script setup lang="ts">
import { computed } from 'vue'
import {
  Collection,
  Connection,
  Files,
  HomeFilled,
  Lock,
  Menu,
  Notebook,
  Reading,
  Refresh,
  Search,
  SwitchButton,
  TrendCharts,
  User,
} from '@element-plus/icons-vue'
import type { AuthUser, RuntimeStatus } from '../../api'
import { getWorkspaceMeta, type WorkspaceView } from './workspace'

const props = defineProps<{
  activeView: WorkspaceView
  sidebarOpen: boolean
  canManageContent: boolean
  isAdmin: boolean
  wrongCount: number
  pendingCount: number
  runtimeStatus: RuntimeStatus | null
  currentUser: AuthUser
  logoUrl: string
  userInitial: string
  roleLabel: string
  currentMaterialTitle?: string
}>()

const emit = defineEmits<{
  navigate: [view: WorkspaceView]
  refresh: []
  accountCommand: [command: 'password' | 'logout']
  'update:sidebarOpen': [open: boolean]
}>()

const viewMeta = computed(() => getWorkspaceMeta(props.activeView, props.canManageContent))

function navigate(view: WorkspaceView) {
  emit('navigate', view)
}

function accountCommand(command: string) {
  if (command === 'password' || command === 'logout') {
    emit('accountCommand', command)
  }
}
</script>

<template>
  <div class="app-layout">
    <button
      v-if="sidebarOpen"
      class="sidebar-overlay"
      type="button"
      aria-label="关闭导航"
      @click="emit('update:sidebarOpen', false)"
    ></button>

    <aside :class="['app-sidebar', { open: sidebarOpen }]">
      <button class="sidebar-brand" type="button" @click="navigate('dashboard')">
        <span class="brand-mark"><img :src="logoUrl" alt="SLAS 标志" /></span>
        <span class="sidebar-brand__copy">
          <strong>SLAS 智学系统</strong>
          <small>Evidence-first learning</small>
        </span>
      </button>

      <div class="sidebar-intro">
        <span>{{ canManageContent ? '教学工作室' : '学习工作室' }}</span>
        <p v-if="canManageContent">资料、证据、题目与练习，在同一条学习链路中保持可追溯。</p>
        <p v-else>练习、错题与掌握度会随每次提交持续更新。</p>
      </div>

      <nav class="sidebar-nav" aria-label="主导航">
        <div class="nav-group">
          <span class="nav-group__label">日常工作</span>
          <button :class="{ active: activeView === 'dashboard' }" type="button" @click="navigate('dashboard')">
            <el-icon><HomeFilled /></el-icon><span>工作概览</span>
          </button>
          <button :class="{ active: activeView === 'learning' }" type="button" @click="navigate('learning')">
            <el-icon><Reading /></el-icon><span>{{ canManageContent ? '练习任务' : '我的练习' }}</span>
          </button>
          <button :class="{ active: activeView === 'progress' }" type="button" @click="navigate('progress')">
            <el-icon><TrendCharts /></el-icon><span>学习画像</span>
            <em v-if="wrongCount > 0" aria-label="错题数量">{{ wrongCount }}</em>
          </button>
        </div>

        <div v-if="canManageContent" class="nav-group">
          <span class="nav-group__label">内容生产</span>
          <button :class="{ active: activeView === 'courses' }" type="button" @click="navigate('courses')">
            <el-icon><Collection /></el-icon><span>课程与知识点</span>
          </button>
          <button :class="{ active: activeView === 'materials' }" type="button" @click="navigate('materials')">
            <el-icon><Files /></el-icon><span>教材知识库</span>
          </button>
          <button :class="{ active: activeView === 'rag' }" type="button" @click="navigate('rag')">
            <el-icon><Search /></el-icon><span>检索验证</span>
          </button>
          <button :class="{ active: activeView === 'questions' }" type="button" @click="navigate('questions')">
            <el-icon><Notebook /></el-icon><span>智能题库</span>
            <em v-if="pendingCount > 0" aria-label="待审核题目数量">{{ pendingCount }}</em>
          </button>
          <button :class="{ active: activeView === 'agents' }" type="button" @click="navigate('agents')">
            <el-icon><Connection /></el-icon><span>智能体运行</span>
          </button>
        </div>

        <div v-if="isAdmin" class="nav-group">
          <span class="nav-group__label">系统管理</span>
          <button :class="{ active: activeView === 'users' }" type="button" @click="navigate('users')">
            <el-icon><User /></el-icon><span>账号与权限</span>
          </button>
        </div>
      </nav>

      <div class="sidebar-status">
        <span :class="['status-dot', { ready: runtimeStatus }]" aria-hidden="true"></span>
        <div>
          <span class="sidebar-status__label">SYSTEM STATUS</span>
          <strong>{{ runtimeStatus ? '服务链路就绪' : '正在检查服务' }}</strong>
          <small>{{ runtimeStatus?.aiProvider || 'AI' }} · {{ runtimeStatus?.vectorProvider || 'Vector DB' }}</small>
        </div>
      </div>
    </aside>

    <section class="workspace-shell">
      <header class="app-topbar">
        <el-tooltip content="打开导航" placement="bottom">
          <button class="icon-button mobile-menu" type="button" aria-label="打开导航" @click="emit('update:sidebarOpen', true)">
            <el-icon><Menu /></el-icon>
          </button>
        </el-tooltip>

        <div class="page-context">
          <span class="page-context__eyebrow">{{ viewMeta.eyebrow }}</span>
          <div class="page-context__title-row">
            <h1>{{ viewMeta.title }}</h1>
            <span class="trace-chip"><i></i> 可追溯工作区</span>
          </div>
          <p>{{ viewMeta.description }}</p>
        </div>

        <div class="topbar-actions">
          <button v-if="canManageContent" class="scope-switch" type="button" @click="navigate('materials')">
            <span>CURRENT MATERIAL</span>
            <strong>{{ currentMaterialTitle || '尚未选择教材' }}</strong>
          </button>
          <el-tooltip content="刷新当前数据" placement="bottom">
            <button class="icon-button" type="button" aria-label="刷新当前数据" @click="emit('refresh')">
              <el-icon><Refresh /></el-icon>
            </button>
          </el-tooltip>
          <el-dropdown trigger="click" placement="bottom-end" @command="accountCommand">
            <button class="user-menu" type="button" aria-label="打开账号菜单">
              <span class="avatar">{{ userInitial }}</span>
              <span class="user-menu__copy">
                <strong>{{ currentUser.realName || currentUser.username }}</strong>
                <small>{{ roleLabel }}</small>
              </span>
              <span class="user-menu__caret">⌄</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password"><el-icon><Lock /></el-icon>修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="main-content">
        <slot />
      </main>
    </section>
  </div>
</template>
