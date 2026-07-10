<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Cpu, DataAnalysis, Document, MagicStick, Search, UploadFilled } from '@element-plus/icons-vue'
import type {
  AgentRunRecord,
  AgentWorkflowTemplate,
  MaterialChunk,
  MaterialRecord,
  QuestionDifficulty,
  QuestionRecord,
  QuestionType,
  RetrievalHit,
  SubjectPreset,
} from './api'
import {
  approveQuestion,
  generateQuestions,
  getAgentRun,
  getWorkflowTemplate,
  listAgentRuns,
  listChunks,
  listMaterials,
  listQuestions,
  parseMaterial,
  rejectQuestion,
  retrieve,
  uploadMaterial,
} from './api'

const materials = ref<MaterialRecord[]>([])
const chunks = ref<MaterialChunk[]>([])
const questions = ref<QuestionRecord[]>([])
const hits = ref<RetrievalHit[]>([])
const agentRuns = ref<AgentRunRecord[]>([])
const workflow = ref<AgentWorkflowTemplate | null>(null)
const currentRun = ref<AgentRunRecord | null>(null)
const currentMaterial = ref<MaterialRecord | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const title = ref('')
const subjectPreset = ref<SubjectPreset>('GENERAL')
const query = ref('')
const topic = ref('')
const difficulty = ref<QuestionDifficulty>('MEDIUM')
const count = ref(5)
const questionTypes = ref<QuestionType[]>(['SINGLE_CHOICE', 'SHORT_ANSWER', 'FILL_BLANK'])
const loading = ref(false)

const subjectOptions = [
  { label: '通用学习', value: 'GENERAL' },
  { label: '英语学习', value: 'ENGLISH' },
  { label: '商务/外贸英语', value: 'BUSINESS_ENGLISH' },
  { label: '计算机课程', value: 'COMPUTER' },
  { label: '自定义课程', value: 'CUSTOM' },
] as const

const questionTypeOptions = [
  { label: '单选', value: 'SINGLE_CHOICE' },
  { label: '判断', value: 'TRUE_FALSE' },
  { label: '填空', value: 'FILL_BLANK' },
  { label: '简答', value: 'SHORT_ANSWER' },
  { label: '翻译/转述', value: 'TRANSLATION' },
  { label: '听写', value: 'DICTATION' },
  { label: '口语表达', value: 'SPEAKING_PROMPT' },
  { label: '写作', value: 'WRITING' },
] as const

const indexedCount = computed(() => materials.value.filter((item) => item.status === 'INDEXED').length)
const pendingCount = computed(() => questions.value.filter((item) => item.status === 'PENDING_REVIEW').length)
const approvedCount = computed(() => questions.value.filter((item) => item.status === 'APPROVED').length)

onMounted(async () => {
  await refreshAll()
})

async function refreshAll() {
  try {
    const [materialList, questionList, workflowTemplate, runs] = await Promise.all([
      listMaterials(),
      listQuestions(),
      getWorkflowTemplate(),
      listAgentRuns(),
    ])
    materials.value = materialList
    questions.value = questionList
    workflow.value = workflowTemplate
    agentRuns.value = runs
    if (!currentMaterial.value && materialList.length > 0) {
      await selectMaterial(materialList[0])
    }
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

function chooseFile() {
  fileInput.value?.click()
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  if (selectedFile.value && !title.value) {
    title.value = selectedFile.value.name.replace(/\.[^.]+$/, '')
  }
}

async function uploadAndParse() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择教材文件')
    return
  }
  loading.value = true
  try {
    const uploaded = await uploadMaterial(selectedFile.value, title.value, subjectPreset.value)
    const indexed = await parseMaterial(uploaded.materialId)
    ElMessage.success(`教材已解析，生成 ${indexed.chunkCount} 个切片`)
    selectedFile.value = null
    title.value = ''
    await refreshAll()
    await selectMaterial(indexed)
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    loading.value = false
  }
}

async function selectMaterial(material: MaterialRecord) {
  currentMaterial.value = material
  topic.value = topic.value || material.title
  query.value = query.value || material.title
  try {
    chunks.value = await listChunks(material.materialId)
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function runRetrieve() {
  if (!currentMaterial.value) return
  loading.value = true
  try {
    const result = await retrieve(query.value || currentMaterial.value.title, currentMaterial.value.materialId, 6)
    hits.value = result.hits
    ElMessage.success(`检索到 ${result.hits.length} 条教材证据`)
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    loading.value = false
  }
}

async function runGenerate() {
  if (!currentMaterial.value) {
    ElMessage.warning('请先选择已解析教材')
    return
  }
  loading.value = true
  try {
    const task = await generateQuestions({
      materialId: currentMaterial.value.materialId,
      subjectPreset: currentMaterial.value.subjectPreset,
      topic: topic.value || currentMaterial.value.title,
      questionTypes: questionTypes.value,
      difficulty: difficulty.value,
      count: count.value,
    })
    questions.value = await listQuestions()
    agentRuns.value = await listAgentRuns()
    currentRun.value = task.agentRunId ? await getAgentRun(task.agentRunId) : null
    ElMessage.success(`已生成 ${task.questions.length} 道待审核题目`)
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    loading.value = false
  }
}

async function updateStatus(question: QuestionRecord, approved: boolean) {
  try {
    await (approved ? approveQuestion(question.questionId) : rejectQuestion(question.questionId))
    questions.value = await listQuestions()
    ElMessage.success(approved ? '题目已通过' : '题目已退回')
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function openRun(run: AgentRunRecord) {
  currentRun.value = await getAgentRun(run.runId)
}

function messageOf(error: unknown) {
  return error instanceof Error ? error.message : String(error)
}
</script>

<template>
  <main class="app-shell">
    <div class="ambient ambient-one"></div>
    <div class="ambient ambient-two"></div>

    <section class="topbar glass-card">
      <div class="brand">
        <div class="brand-mark">SL</div>
        <div>
          <div class="brand-title">Smart Learning Agent</div>
          <div class="brand-subtitle">通用学习 · RAG · 多智能体出题</div>
        </div>
      </div>
      <div class="decor-nav">
        <a href="#materials">教材知识库</a>
        <a href="#rag">向量检索</a>
        <a href="#questions">题库审核</a>
        <a href="#agents">智能体链路</a>
      </div>
    </section>

    <section class="hero glass-card">
      <div>
        <p class="eyebrow">Graduation Design Workspace</p>
        <h1>把任意教材变成可追溯、可审核、可练习的智能题库</h1>
        <p class="hero-copy">系统不绑定商务英语：英语、计算机、通识课或自定义教材都可以先切片入库，再通过向量检索和多智能体工作流生成习题。</p>
      </div>
      <div class="metric-grid">
        <div class="metric-card"><span>{{ materials.length }}</span><small>教材</small></div>
        <div class="metric-card"><span>{{ indexedCount }}</span><small>已索引</small></div>
        <div class="metric-card"><span>{{ pendingCount }}</span><small>待审核题</small></div>
        <div class="metric-card"><span>{{ approvedCount }}</span><small>已通过</small></div>
      </div>
    </section>

    <section id="materials" class="workspace-grid">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><UploadFilled /></el-icon>教材上传与解析</div></template>
        <input ref="fileInput" type="file" class="hidden-input" accept=".pdf,.docx,.txt,.md,.xlsx,.xls" @change="onFileChange" />
        <div class="upload-box" @click="chooseFile">
          <el-icon><Document /></el-icon>
          <strong>{{ selectedFile?.name || '选择教材文件' }}</strong>
          <span>支持 PDF、DOCX、TXT、Markdown、Excel</span>
        </div>
        <el-input v-model="title" placeholder="教材标题，可自动使用文件名" />
        <el-select v-model="subjectPreset" class="wide-select">
          <el-option v-for="item in subjectOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="uploadAndParse">上传并解析入库</el-button>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><DataAnalysis /></el-icon>教材列表</div></template>
        <div class="material-list">
          <button v-for="item in materials" :key="item.materialId" :class="['material-item', { active: item.materialId === currentMaterial?.materialId }]" @click="selectMaterial(item)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.subjectPreset }} · {{ item.status }} · {{ item.chunkCount }} chunks</span>
          </button>
        </div>
      </el-card>
    </section>

    <section id="rag" class="workspace-grid">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><Search /></el-icon>向量检索验证</div></template>
        <el-input v-model="query" type="textarea" :rows="3" placeholder="输入要检索的学习主题，例如：报价邮件、被动语态、二叉树遍历" />
        <el-button type="primary" :disabled="!currentMaterial" :loading="loading" @click="runRetrieve">检索教材证据</el-button>
        <div class="hit-list">
          <article v-for="hit in hits" :key="hit.chunk.chunkId" class="hit-card">
            <div class="hit-score">score {{ hit.score.toFixed(3) }}</div>
            <p>{{ hit.chunk.text }}</p>
          </article>
        </div>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><Document /></el-icon>当前教材切片</div></template>
        <div class="chunk-list">
          <article v-for="chunk in chunks.slice(0, 5)" :key="chunk.chunkId" class="chunk-card">
            <strong>#{{ chunk.chunkIndex }} {{ chunk.chapterTitle }}</strong>
            <p>{{ chunk.text }}</p>
            <el-tag v-for="keyword in chunk.keywords.slice(0, 5)" :key="keyword" size="small">{{ keyword }}</el-tag>
          </article>
        </div>
      </el-card>
    </section>

    <section id="questions" class="workspace-grid wide">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><MagicStick /></el-icon>生成习题</div></template>
        <el-input v-model="topic" placeholder="出题主题，例如：外贸报价、英语时态、软件测试基础" />
        <el-checkbox-group v-model="questionTypes">
          <el-checkbox-button v-for="item in questionTypeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-checkbox-button>
        </el-checkbox-group>
        <div class="inline-form">
          <el-select v-model="difficulty"><el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" /></el-select>
          <el-input-number v-model="count" :min="1" :max="20" />
          <el-button type="primary" :disabled="!currentMaterial" :loading="loading" @click="runGenerate">启动多智能体生成</el-button>
        </div>
      </el-card>

      <el-card class="panel question-panel" shadow="never">
        <template #header><div class="panel-title">题库审核</div></template>
        <article v-for="question in questions" :key="question.questionId" class="question-card">
          <div class="question-head">
            <el-tag>{{ question.type }}</el-tag>
            <el-tag type="warning">{{ question.difficulty }}</el-tag>
            <el-tag :type="question.status === 'APPROVED' ? 'success' : question.status === 'REJECTED' ? 'danger' : 'info'">{{ question.status }}</el-tag>
          </div>
          <p class="question-prompt">{{ question.prompt }}</p>
          <div v-if="question.options.length" class="option-list">
            <span v-for="option in question.options" :key="option.label">{{ option.label }}. {{ option.text }}</span>
          </div>
          <p><strong>答案：</strong>{{ question.answerText }}</p>
          <p><strong>解析：</strong>{{ question.analysisText }}</p>
          <p class="source-line"><strong>来源：</strong>{{ question.sourceRefs[0]?.chapterTitle }} · {{ question.sourceRefs[0]?.snippet }}</p>
          <div class="card-actions">
            <el-button size="small" type="success" @click="updateStatus(question, true)">通过</el-button>
            <el-button size="small" type="danger" plain @click="updateStatus(question, false)">退回</el-button>
          </div>
        </article>
      </el-card>
    </section>

    <section id="agents" class="workspace-grid">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><Cpu /></el-icon>多智能体工作流</div></template>
        <div v-for="step in workflow?.steps" :key="step.role" class="workflow-step">
          <strong>{{ step.role }}</strong>
          <span>{{ step.goal }}</span>
          <small>{{ step.toolNames.join(' / ') }}</small>
        </div>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title">运行记录</div></template>
        <button v-for="run in agentRuns" :key="run.runId" class="run-item" @click="openRun(run)">
          <strong>{{ run.status }}</strong>
          <span>{{ run.finalAnswer }}</span>
        </button>
        <div v-if="currentRun" class="run-detail">
          <h3>当前运行</h3>
          <p>{{ currentRun.finalAnswer }}</p>
          <div v-for="step in currentRun.steps" :key="step.stepId" class="workflow-step done">
            <strong>{{ step.role }}</strong>
            <span>{{ step.summary }}</span>
          </div>
        </div>
      </el-card>
    </section>
  </main>
</template>
