export type SubjectPreset = 'GENERAL' | 'ENGLISH' | 'BUSINESS_ENGLISH' | 'COMPUTER' | 'CUSTOM'
export type MaterialStatus = 'UPLOADED' | 'PARSING' | 'INDEXED' | 'FAILED'
export type QuestionType = 'SINGLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER' | 'TRANSLATION' | 'DICTATION' | 'SPEAKING_PROMPT' | 'WRITING'
export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type QuestionStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'
export type AgentRunStatus = 'RUNNING' | 'FINISHED' | 'FAILED'
export type PracticeSetStatus = 'ACTIVE' | 'FINISHED'

const TOKEN_KEY = 'smart-learning-agent-token'

export interface AuthUser {
  userId: string
  username: string
  realName: string | null
  roles: string[]
  lastLoginAt: string | null
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresAt: string
  user: AuthUser
}

export interface CourseRecord {
  courseId: string
  name: string
  subjectPreset: SubjectPreset
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface CourseChapterRecord {
  chapterId: string
  courseId: string
  title: string
  chapterOrder: number
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface MaterialCourseBindingRecord {
  materialId: string
  courseId: string
  chapterId: string | null
  createdAt: string
  updatedAt: string
}

export interface KnowledgePointRecord {
  knowledgePointId: string
  courseId: string | null
  chapterId: string | null
  materialId: string
  chunkId: string
  name: string
  description: string
  sourceSnippet: string
  weight: number
  createdAt: string
}

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


export interface UpdateQuestionPayload {
  prompt: string
  options: QuestionOption[]
  answerText: string
  analysisText: string
  difficulty: QuestionDifficulty
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

export interface RuntimeStatus {
  aiProvider: string
  aiModel: string
  aiApiKeyConfigured: boolean
  aiBaseUrl: string
  embeddingProvider: string
  embeddingModel: string
  embeddingApiKeyConfigured: boolean
  vectorProvider: string
  vectorCollectionName: string
  qdrantUrl: string
  vectorDimension: number
  defaultTopK: number
}

export interface PracticeSetRecord {
  practiceId: string
  title: string
  studentId: string
  courseId: string | null
  chapterId: string | null
  materialId: string | null
  questionIds: string[]
  status: PracticeSetStatus
  createdAt: string
  updatedAt: string
}

export interface PracticeAttemptRecord {
  attemptId: string
  practiceId: string
  studentId: string
  questionId: string
  answerText: string
  correct: boolean
  score: number
  expectedAnswer: string
  feedback: string
  knowledgeNames: string[]
  submittedAt: string
}

export interface WrongQuestionRecord {
  studentId: string
  questionId: string
  materialId: string
  prompt: string
  expectedAnswer: string
  lastAnswer: string
  lastFeedback: string
  wrongCount: number
  lastSubmittedAt: string
}

export interface KnowledgeMasteryRecord {
  studentId: string
  courseId: string | null
  chapterId: string | null
  materialId: string | null
  knowledgeName: string
  totalAttempts: number
  correctAttempts: number
  mastery: number
  updatedAt: string
}

export interface PracticeDetail {
  practice: PracticeSetRecord
  questions: QuestionRecord[]
  attempts: PracticeAttemptRecord[]
}

export interface PracticeResult {
  practiceId: string
  attemptId: string
  questionId: string
  correct: boolean
  score: number
  expectedAnswer: string
  feedback: string
  knowledgeNames: string[]
}

type RequestOptions = RequestInit & { skipAuth?: boolean }

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setStoredToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY)
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { skipAuth, ...fetchOptions } = options
  const headers = new Headers(fetchOptions.headers || {})
  const token = getStoredToken()
  if (!skipAuth && token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (fetchOptions.body && !(fetchOptions.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(url, { ...fetchOptions, headers })
  if (!response.ok) {
    let message = `请求失败：${response.status}`
    try {
      const error = await response.json()
      message = error.message ?? message
    } catch {
      // ignore non-json errors
    }
    if (response.status === 401) {
      clearStoredToken()
    }
    throw new Error(message)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export function loginWithPassword(payload: { username: string; password: string }) {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
    skipAuth: true,
  })
}

export function getCurrentUser() {
  return request<AuthUser>('/api/auth/me')
}

export function logout() {
  return request<{ success: boolean }>('/api/auth/logout', { method: 'POST', skipAuth: true })
}


export function createCourse(payload: { name: string; subjectPreset: SubjectPreset; description?: string }) {
  return request<CourseRecord>('/api/courses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function listCourses() {
  return request<CourseRecord[]>('/api/courses')
}

export function createChapter(courseId: string, payload: { title: string; chapterOrder?: number; description?: string }) {
  return request<CourseChapterRecord>(`/api/courses/${courseId}/chapters`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function listChapters(courseId: string) {
  return request<CourseChapterRecord[]>(`/api/courses/${courseId}/chapters`)
}

export function bindMaterialToCourse(materialId: string, courseId: string, chapterId?: string) {
  return request<MaterialCourseBindingRecord>(`/api/materials/${materialId}/course-binding`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ courseId, chapterId }),
  })
}

export function refreshKnowledgePoints(materialId: string) {
  return request<KnowledgePointRecord[]>(`/api/materials/${materialId}/knowledge-points/refresh`, { method: 'POST' })
}

export function listKnowledgePoints(filters: { courseId?: string; chapterId?: string; materialId?: string } = {}) {
  const params = new URLSearchParams()
  if (filters.courseId) params.set('courseId', filters.courseId)
  if (filters.chapterId) params.set('chapterId', filters.chapterId)
  if (filters.materialId) params.set('materialId', filters.materialId)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return request<KnowledgePointRecord[]>(`/api/knowledge-points${suffix}`)
}

export function uploadMaterial(file: File, title: string, subjectPreset: SubjectPreset, courseId?: string, chapterId?: string) {
  const body = new FormData()
  body.append('file', file)
  body.append('title', title)
  body.append('subjectPreset', subjectPreset)
  if (courseId) body.append('courseId', courseId)
  if (chapterId) body.append('chapterId', chapterId)
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

export function updateQuestion(questionId: string, payload: UpdateQuestionPayload) {
  return request<QuestionRecord>(`/api/questions/${questionId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function batchUpdateQuestionStatus(questionIds: string[], approved: boolean) {
  return request<QuestionRecord[]>('/api/questions/batch-status', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ questionIds, approved }),
  })
}

export function exportQuestions(materialId?: string, status?: QuestionStatus) {
  const params = new URLSearchParams()
  if (materialId) params.set('materialId', materialId)
  if (status) params.set('status', status)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  const headers = new Headers()
  const token = getStoredToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  return fetch(`/api/questions/export${suffix}`, { headers }).then((response) => {
    if (response.status === 401) clearStoredToken()
    if (!response.ok) throw new Error(`Export failed: ${response.status}`)
    return response.blob()
  })
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

export function createPracticeSet(payload: {
  studentId?: string
  courseId?: string
  chapterId?: string
  materialId?: string
  count?: number
}) {
  return request<PracticeDetail>('/api/practice/sets', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function listPracticeSets(studentId?: string) {
  const params = new URLSearchParams()
  if (studentId) params.set('studentId', studentId)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return request<PracticeSetRecord[]>(`/api/practice/sets${suffix}`)
}

export function getPracticeSet(practiceId: string) {
  return request<PracticeDetail>(`/api/practice/sets/${practiceId}`)
}

export function submitPractice(payload: {
  practiceId?: string
  questionId: string
  studentId?: string
  answerText?: string
}) {
  return request<PracticeResult>('/api/practice/submit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function listPracticeAttempts(filters: { practiceId?: string; studentId?: string } = {}) {
  const params = new URLSearchParams()
  if (filters.practiceId) params.set('practiceId', filters.practiceId)
  if (filters.studentId) params.set('studentId', filters.studentId)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return request<PracticeAttemptRecord[]>(`/api/practice/attempts${suffix}`)
}

export function listWrongQuestions(studentId?: string) {
  const params = new URLSearchParams()
  if (studentId) params.set('studentId', studentId)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return request<WrongQuestionRecord[]>(`/api/practice/wrong-questions${suffix}`)
}

export function listMastery(studentId?: string) {
  const params = new URLSearchParams()
  if (studentId) params.set('studentId', studentId)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return request<KnowledgeMasteryRecord[]>(`/api/practice/mastery${suffix}`)
}

export function getRuntimeStatus() {
  return request<RuntimeStatus>('/api/runtime/status')
}
