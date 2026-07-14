<script setup lang="ts">
import { computed } from 'vue'
import { Cpu, DataAnalysis, Timer, Tools } from '@element-plus/icons-vue'
import type { AgentRole, AgentRunRecord, AgentWorkflowTemplate } from '../../api'

const props = defineProps<{
  workflow: AgentWorkflowTemplate | null
  runs: AgentRunRecord[]
  currentRun: AgentRunRecord | null
}>()

const emit = defineEmits<{
  selectRun: [run: AgentRunRecord]
}>()

const roleOrder: AgentRole[] = [
  'MATERIAL_UNDERSTANDING',
  'RETRIEVAL_PLANNING',
  'QUESTION_GENERATION',
  'QUALITY_REVIEW',
  'DEDUP_DIFFICULTY',
  'TEACHING_COMPOSER',
]

const roleMeta: Record<AgentRole, { label: string; description: string }> = {
  MATERIAL_UNDERSTANDING: { label: '教材理解', description: '识别教材主题、知识点与可用来源片段' },
  RETRIEVAL_PLANNING: { label: '检索规划', description: '生成检索计划并执行教材证据召回' },
  QUESTION_GENERATION: { label: '习题生成', description: '基于检索证据生成结构化候选题' },
  QUALITY_REVIEW: { label: '质量审校', description: '独立检查来源、答案、格式与可审核性' },
  DEDUP_DIFFICULTY: { label: '去重与难度', description: '识别重复题并评估或校正题目难度' },
  TEACHING_COMPOSER: { label: '教学编排', description: '整理最终候选题与教师审核建议' },
}

const orderedWorkflow = computed(() => {
  const steps = props.workflow?.steps ?? []
  return [...steps].sort((a, b) => roleOrder.indexOf(a.role as AgentRole) - roleOrder.indexOf(b.role as AgentRole))
})

function roleLabel(role: string) {
  return roleMeta[role as AgentRole]?.label ?? role
}

function roleDescription(role: string, fallback: string) {
  return roleMeta[role as AgentRole]?.description ?? fallback
}

function statusLabel(status: AgentRunRecord['status']) {
  if (status === 'FINISHED') return '执行完成'
  if (status === 'FAILED') return '执行失败'
  return '执行中'
}

function statusType(status: AgentRunRecord['status']) {
  if (status === 'FINISHED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function modeLabel(mode?: string) {
  if (mode === 'DIRECT') return '直接生成'
  if (mode === 'RAG_ONLY') return '仅 RAG'
  return 'RAG 多智能体'
}

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString() : '尚未结束'
}

function stepToolCount(run: AgentRunRecord, role: string) {
  return run.toolCalls.filter((call) => call.role === role).length
}
</script>

<template>
  <section id="agents" class="workspace-grid agents-workspace view-section">
    <el-card class="panel agent-blueprint" shadow="never">
      <template #header>
        <div class="panel-title agent-panel-title">
          <span><el-icon><Cpu /></el-icon>协同角色链</span>
          <small>{{ orderedWorkflow.length }} 个可独立追踪角色</small>
        </div>
      </template>

      <div class="agent-blueprint__intro">
        <strong>不是预设日志，而是逐角色真实执行</strong>
        <p>每一步读取前序输出，记录结构化结果、执行时间与实际工具调用；失败后不会伪装完成后续步骤。</p>
      </div>

      <ol v-if="orderedWorkflow.length" class="agent-role-list">
        <li v-for="(step, index) in orderedWorkflow" :key="step.role">
          <span class="agent-role-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <div>
            <strong>{{ roleLabel(step.role) }}</strong>
            <p>{{ roleDescription(step.role, step.goal) }}</p>
            <small><el-icon><Tools /></el-icon>{{ step.toolNames.join(' / ') }}</small>
          </div>
        </li>
      </ol>
      <div v-else class="empty-note">暂无工作流模板，请检查后端智能体配置。</div>
    </el-card>

    <el-card class="panel agent-runs" shadow="never">
      <template #header>
        <div class="panel-title agent-panel-title">
          <span><el-icon><DataAnalysis /></el-icon>运行追踪</span>
          <small>{{ runs.length }} 次运行</small>
        </div>
      </template>

      <div v-if="runs.length" class="agent-run-list">
        <button v-for="run in runs" :key="run.runId" type="button" class="run-item" @click="emit('selectRun', run)">
          <span class="run-item__head">
            <el-tag :type="statusType(run.status)" effect="light">{{ statusLabel(run.status) }}</el-tag>
            <small>{{ modeLabel(run.workflowMode) }}</small>
          </span>
          <strong>{{ run.objective }}</strong>
          <span>{{ run.finalAnswer || run.errorSummary || '运行结果尚未生成' }}</span>
          <time>{{ formatTime(run.createdAt) }}</time>
        </button>
      </div>
      <div v-else class="empty-note">暂无运行记录。完成一次题目生成后，这里会展示真实角色链路。</div>

      <div v-if="currentRun" class="run-detail">
        <div class="run-detail__header">
          <div>
            <span class="section-kicker">SELECTED RUN</span>
            <h3>{{ currentRun.objective }}</h3>
          </div>
          <el-tag :type="statusType(currentRun.status)" effect="dark">{{ statusLabel(currentRun.status) }}</el-tag>
        </div>

        <div class="run-facts">
          <div><small>运行模式</small><strong>{{ modeLabel(currentRun.workflowMode) }}</strong></div>
          <div><small>实际工具调用</small><strong>{{ currentRun.toolCalls.length }}</strong></div>
          <div><small>总耗时</small><strong>{{ currentRun.metrics?.durationMs ?? 0 }} ms</strong></div>
          <div><small>有证据题目</small><strong>{{ currentRun.metrics?.groundedQuestionCount ?? 0 }}</strong></div>
        </div>

        <div v-if="currentRun.errorSummary" class="run-error">
          <strong>失败角色：{{ roleLabel(currentRun.failedRole || '') }}</strong>
          <span>{{ currentRun.errorSummary }}</span>
        </div>

        <div class="run-step-list">
          <article v-for="step in currentRun.steps" :key="step.stepId" :class="['run-step', `is-${step.status.toLowerCase()}`]">
            <span class="run-step__marker"></span>
            <div>
              <div class="run-step__head">
                <strong>{{ roleLabel(step.role) }}</strong>
                <el-tag size="small" :type="statusType(step.status)" effect="plain">{{ statusLabel(step.status) }}</el-tag>
              </div>
              <p>{{ step.summary || step.errorSummary || '等待角色输出' }}</p>
              <small>
                <el-icon><Timer /></el-icon>{{ formatTime(step.startedAt) }} · {{ stepToolCount(currentRun, step.role) }} 次实际调用
              </small>
            </div>
          </article>
        </div>
      </div>
    </el-card>
  </section>
</template>
