export type SubjectPreset = 'GENERAL' | 'ENGLISH' | 'BUSINESS_ENGLISH' | 'COMPUTER' | 'CUSTOM'
export type MaterialStatus = 'UPLOADED' | 'PARSING' | 'INDEXED' | 'FAILED'
export type QuestionType = 'SINGLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER' | 'TRANSLATION' | 'DICTATION' | 'SPEAKING_PROMPT' | 'WRITING'
export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type QuestionStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'
export type AgentRunStatus = 'RUNNING' | 'FINISHED' | 'FAILED'

export interface MaterialRecord {
  materialId: string
  title: string
  subjectPreset: SubjectPreset
  originalFileName: string
  contentType: string | null
  status: MaterialStatus
  chunkCount: number
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface MaterialChunk {
  chunkId: string
  materialId: string
  chapterId: string
  chapterTitle: string
  chunkIndex: number
  pageNo: number | null
  sourceLabel: string
  text: string
  keywords: string[]
  createdAt: string
}

export interface RetrievalHit {
  chunk: MaterialChunk
  score: number
  reason: string
}

export interface RetrievalResult {
  query: string
  hits: RetrievalHit[]
}

export interface QuestionOption {
  label: string
  text: string
  correct: boolean
}

export interface QuestionSourceRef {
  materialId: string
  chunkId: string
  chapterTitle: string
  pageNo: number | null
  snippet: string
  score: number
}

export interface QuestionRecord {
  questionId: string
  taskId: string
  materialId: string
  type: QuestionType
  difficulty: QuestionDifficulty
  subjectPreset: SubjectPreset
  prompt: string
  options: QuestionOption[]
  answerText: string
  analysisText: string
  sourceRefs: QuestionSourceRef[]
  status: QuestionStatus
  createdAt: string
  updatedAt: string
}

export interface GenerationTaskRecord {
  taskId: string
  materialId: string
  subjectPreset: SubjectPreset
  topic: string
  questionTypes: QuestionType[]
  difficulty: QuestionDifficulty
  requestedCount: number
  status: string
  agentRunId: string
  questions: QuestionRecord[]
  createdAt: string
  updatedAt: string
}

export interface AgentStep {
  role: string
  goal: string
  toolNames: string[]
}

export interface AgentWorkflowTemplate {
  workflowId: string
  title: string
  steps: AgentStep[]
}

export interface AgentStepReport {
  stepId: string
  role: string
  goal: string
  summary: string
  status: AgentRunStatus
}

export interface AgentToolCall {
  toolName: string
  input: string
  output: string
  success: boolean
}

export interface AgentRunRecord {
  runId: string
  taskId: string
  objective: string
  status: AgentRunStatus
  steps: AgentStepReport[]
  toolCalls: AgentToolCall[]
  finalAnswer: string
  createdAt: string
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, options)
  if (!response.ok) {
    let message = `请求失败：${response.status}`
    try {
      const error = await response.json()
      message = error.message ?? message
    } catch {
      // ignore non-json errors
    }
    throw new Error(message)
  }
  return response.json() as Promise<T>
}

export function uploadMaterial(file: File, title: string, subjectPreset: SubjectPreset) {
  const body = new FormData()
  body.append('file', file)
  body.append('title', title)
  body.append('subjectPreset', subjectPreset)
  return request<MaterialRecord>('/api/materials/upload', { method: 'POST', body })
}

export function parseMaterial(materialId: string) {
  return request<MaterialRecord>(`/api/materials/${materialId}/parse`, { method: 'POST' })
}

export function listMaterials() {
  return request<MaterialRecord[]>('/api/materials')
}

export function listChunks(materialId: string) {
  return request<MaterialChunk[]>(`/api/materials/${materialId}/chunks`)
}

export function retrieve(query: string, materialId: string, topK = 6) {
  return request<RetrievalResult>('/api/rag/retrieve', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, materialId, topK }),
  })
}

export function generateQuestions(payload: {
  materialId: string
  subjectPreset: SubjectPreset
  topic: string
  questionTypes: QuestionType[]
  difficulty: QuestionDifficulty
  count: number
}) {
  return request<GenerationTaskRecord>('/api/questions/generation-tasks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function listQuestions() {
  return request<QuestionRecord[]>('/api/questions')
}

export function approveQuestion(questionId: string) {
  return request<QuestionRecord>(`/api/questions/${questionId}/approve`, { method: 'POST' })
}

export function rejectQuestion(questionId: string) {
  return request<QuestionRecord>(`/api/questions/${questionId}/reject`, { method: 'POST' })
}

export function getWorkflowTemplate() {
  return request<AgentWorkflowTemplate>('/api/agents/workflow-template')
}

export function listAgentRuns() {
  return request<AgentRunRecord[]>('/api/agents/runs')
}

export function getAgentRun(runId: string) {
  return request<AgentRunRecord>(`/api/agents/runs/${runId}`)
}
