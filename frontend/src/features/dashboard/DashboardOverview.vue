<script setup lang="ts">
import {
  ArrowRight,
  CircleCheckFilled,
  Collection,
  Files,
  MagicStick,
  Notebook,
  Reading,
  Search,
  TrendCharts,
  UploadFilled,
} from '@element-plus/icons-vue'
import type { KnowledgeMasteryRecord, PracticeDetail, RuntimeStatus } from '../../api'

type DashboardTarget = 'courses' | 'materials' | 'rag' | 'questions' | 'learning' | 'progress'

defineProps<{
  canManageContent: boolean
  roleLabel: string
  displayName: string
  greeting: string
  materialsCount: number
  indexedCount: number
  approvedCount: number
  pendingCount: number
  practiceSetCount: number
  practiceAttemptCount: number
  recentAccuracy: number
  averageMastery: number
  wrongCount: number
  masteryRecords: KnowledgeMasteryRecord[]
  hitsCount: number
  currentCourseName?: string
  currentMaterialTitle?: string
  currentPractice: PracticeDetail | null
  runtimeStatus: RuntimeStatus | null
}>()

const emit = defineEmits<{
  navigate: [target: DashboardTarget]
}>()

function masteryType(value: number) {
  if (value >= 0.8) return 'success'
  if (value >= 0.5) return 'warning'
  return 'exception'
}
</script>

<template>
  <section class="dashboard-page">
    <div class="welcome-band">
      <div>
        <span class="section-kicker">{{ roleLabel }}</span>
        <h2>{{ greeting }}，{{ displayName }}</h2>
        <p v-if="canManageContent">今天可以从待审核题目入手，或继续完善教材与知识点。</p>
        <p v-else>继续完成练习，系统会根据答题结果更新你的学习画像。</p>
      </div>
      <div class="welcome-actions">
        <el-button type="primary" @click="emit('navigate', canManageContent ? 'materials' : 'learning')">
          {{ canManageContent ? '管理教材' : '开始练习' }}
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
        <el-button v-if="canManageContent" @click="emit('navigate', 'questions')">查看待审题目</el-button>
        <el-button v-else @click="emit('navigate', 'progress')">查看学习画像</el-button>
      </div>
    </div>

    <div v-if="canManageContent" class="kpi-grid">
      <article class="kpi-item tone-blue">
        <span class="kpi-icon"><Files /></span>
        <div><small>教材总数</small><strong>{{ materialsCount }}</strong><p>{{ indexedCount }} 份已完成索引</p></div>
      </article>
      <article class="kpi-item tone-green">
        <span class="kpi-icon"><CircleCheckFilled /></span>
        <div><small>已通过题目</small><strong>{{ approvedCount }}</strong><p>可直接用于练习组卷</p></div>
      </article>
      <article class="kpi-item tone-yellow">
        <span class="kpi-icon"><Notebook /></span>
        <div><small>待审核题目</small><strong>{{ pendingCount }}</strong><p>建议优先检查来源证据</p></div>
      </article>
      <article class="kpi-item tone-red">
        <span class="kpi-icon"><Reading /></span>
        <div><small>练习任务</small><strong>{{ practiceSetCount }}</strong><p>{{ practiceAttemptCount }} 次答题提交</p></div>
      </article>
    </div>

    <div v-else class="kpi-grid">
      <article class="kpi-item tone-blue">
        <span class="kpi-icon"><Reading /></span>
        <div><small>练习任务</small><strong>{{ practiceSetCount }}</strong><p>继续完成当前学习计划</p></div>
      </article>
      <article class="kpi-item tone-green">
        <span class="kpi-icon"><CircleCheckFilled /></span>
        <div><small>近期正确率</small><strong>{{ recentAccuracy }}%</strong><p>来自 {{ practiceAttemptCount }} 次提交</p></div>
      </article>
      <article class="kpi-item tone-yellow">
        <span class="kpi-icon"><TrendCharts /></span>
        <div><small>平均掌握度</small><strong>{{ averageMastery }}%</strong><p>{{ masteryRecords.length }} 个知识点</p></div>
      </article>
      <article class="kpi-item tone-red">
        <span class="kpi-icon"><Notebook /></span>
        <div><small>累计错题</small><strong>{{ wrongCount }}</strong><p>及时复习会持续减少</p></div>
      </article>
    </div>

    <div v-if="canManageContent" class="dashboard-grid">
      <section class="work-panel workflow-panel">
        <div class="panel-heading">
          <div><span class="section-kicker">CONTENT FLOW</span><h3>内容生产流程</h3></div>
          <small>按步骤完成可确保题目有据可查</small>
        </div>
        <div class="workflow-lane">
          <button type="button" @click="emit('navigate', 'materials')">
            <span class="step-index">01</span><el-icon><UploadFilled /></el-icon>
            <strong>上传教材</strong><small>{{ indexedCount }}/{{ materialsCount }} 已索引</small>
          </button>
          <ArrowRight class="lane-arrow" />
          <button type="button" @click="emit('navigate', 'rag')">
            <span class="step-index">02</span><el-icon><Search /></el-icon>
            <strong>验证检索</strong><small>{{ hitsCount }} 条当前证据</small>
          </button>
          <ArrowRight class="lane-arrow" />
          <button type="button" @click="emit('navigate', 'questions')">
            <span class="step-index">03</span><el-icon><MagicStick /></el-icon>
            <strong>生成审核</strong><small>{{ pendingCount }} 道待处理</small>
          </button>
          <ArrowRight class="lane-arrow" />
          <button type="button" @click="emit('navigate', 'learning')">
            <span class="step-index">04</span><el-icon><Reading /></el-icon>
            <strong>布置练习</strong><small>{{ practiceSetCount }} 个任务</small>
          </button>
        </div>
      </section>

      <section class="work-panel focus-panel">
        <div class="panel-heading">
          <div><span class="section-kicker">CURRENT SCOPE</span><h3>当前内容范围</h3></div>
        </div>
        <div class="scope-summary">
          <span><Collection /></span>
          <div><small>课程</small><strong>{{ currentCourseName || '尚未选择课程' }}</strong></div>
        </div>
        <div class="scope-summary">
          <span><Files /></span>
          <div><small>教材</small><strong>{{ currentMaterialTitle || '尚未选择教材' }}</strong></div>
        </div>
        <button class="text-action" type="button" @click="emit('navigate', 'courses')">调整课程范围 <ArrowRight /></button>
      </section>
    </div>

    <div v-else class="dashboard-grid student-dashboard-grid">
      <section class="work-panel next-practice-panel">
        <div class="panel-heading">
          <div><span class="section-kicker">NEXT TASK</span><h3>继续学习</h3></div>
          <el-tag v-if="currentPractice" type="success" effect="light">{{ currentPractice.practice.status }}</el-tag>
        </div>
        <div v-if="currentPractice" class="next-practice">
          <span class="next-practice__icon"><Reading /></span>
          <div>
            <strong>{{ currentPractice.practice.title }}</strong>
            <p>{{ currentPractice.questions.length }} 道题 · 已提交 {{ currentPractice.attempts.length }} 次</p>
          </div>
          <el-button type="primary" @click="emit('navigate', 'learning')">继续答题</el-button>
        </div>
        <div v-else class="empty-state compact">
          <span><Notebook /></span><strong>暂无练习任务</strong><p>教师发布练习后会显示在这里。</p>
        </div>
      </section>

      <section class="work-panel review-panel">
        <div class="panel-heading">
          <div><span class="section-kicker">REVIEW FIRST</span><h3>优先复习</h3></div>
          <button class="text-action" type="button" @click="emit('navigate', 'progress')">全部画像 <ArrowRight /></button>
        </div>
        <div v-if="masteryRecords.length" class="review-list">
          <div v-for="item in masteryRecords.slice(0, 3)" :key="`${item.materialId}-${item.knowledgeName}`">
            <span>{{ item.knowledgeName }}</span>
            <el-progress :percentage="Math.round(item.mastery * 100)" :stroke-width="7" :show-text="false" :status="masteryType(item.mastery)" />
            <strong>{{ Math.round(item.mastery * 100) }}%</strong>
          </div>
        </div>
        <div v-else class="empty-state compact"><span><TrendCharts /></span><strong>暂无掌握度数据</strong><p>提交练习后自动生成。</p></div>
      </section>
    </div>

    <section class="system-strip">
      <div class="system-strip__title"><span class="status-dot ready"></span><strong>AI 服务链路</strong><small>运行配置</small></div>
      <div><small>大语言模型</small><strong>{{ runtimeStatus?.aiProvider || 'mock' }} / {{ runtimeStatus?.aiModel || '-' }}</strong><span>{{ runtimeStatus?.aiApiKeyConfigured ? '远程服务已配置' : '本地模拟模式' }}</span></div>
      <div><small>向量模型</small><strong>{{ runtimeStatus?.embeddingProvider || 'mock' }} / {{ runtimeStatus?.embeddingModel || '-' }}</strong><span>维度 {{ runtimeStatus?.vectorDimension || 64 }}</span></div>
      <div><small>向量数据库</small><strong>{{ runtimeStatus?.vectorProvider || 'memory' }}</strong><span>{{ runtimeStatus?.vectorCollectionName || '默认集合' }}</span></div>
    </section>
  </section>
</template>
