<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis,
  Document,
  MagicStick,
  Search,
  UploadFilled,
} from '@element-plus/icons-vue'
import DashboardOverview from './features/dashboard/DashboardOverview.vue'
import LoginView from './features/auth/LoginView.vue'
import AgentRunsView from './features/agents/AgentRunsView.vue'
import WorkspaceShell from './features/shell/WorkspaceShell.vue'
import type { WorkspaceView } from './features/shell/workspace'
import type {
  AgentRunRecord,
  AgentWorkflowTemplate,
  AdminUserRecord,
  AuthUser,
  CourseChapterRecord,
  CourseRecord,
  KnowledgePointRecord,
  KnowledgeMasteryRecord,
  MaterialChunk,
  MaterialRecord,
  PracticeAttemptRecord,
  PracticeDetail,
  PracticeResult,
  PracticeSetRecord,
  QuestionDifficulty,
  QuestionOption,
  QuestionRecord,
  QuestionStatus,
  QuestionType,
  RetrievalHit,
  RuntimeStatus,
  RoleOption,
  SubjectPreset,
  UpsertUserPayload,
  WrongQuestionRecord,
} from './api'
import {
  approveQuestion,
  batchUpdateQuestionStatus,
  changePassword,
  clearStoredToken,
  createChapter,
  createAdminUser,
  createCourse,
  createPracticeSet,
  exportQuestions,
  generateQuestions,
  getAgentRun,
  getCurrentUser,
  getPracticeSet,
  getRuntimeStatus,
  getStoredToken,
  getWorkflowTemplate,
  listAdminRoles,
  listAdminUsers,
  listAgentRuns,
  listChapters,
  listCourses,
  listChunks,
  listKnowledgePoints,
  listMastery,
  listMaterials,
  listPracticeAttempts,
  listPracticeSets,
  listQuestions,
  listWrongQuestions,
  loginWithPassword,
  logout as logoutApi,
  parseMaterial,
  refreshKnowledgePoints,
  rejectQuestion,
  resetAdminPassword,
  retrieve,
  submitPractice,
  setStoredToken,
  updateAdminUser,
  updateQuestion,
  uploadMaterial,
} from './api'

const courses = ref<CourseRecord[]>([])
const chapters = ref<CourseChapterRecord[]>([])
const knowledgePoints = ref<KnowledgePointRecord[]>([])
const materials = ref<MaterialRecord[]>([])
const chunks = ref<MaterialChunk[]>([])
const questions = ref<QuestionRecord[]>([])
const hits = ref<RetrievalHit[]>([])
const agentRuns = ref<AgentRunRecord[]>([])
const workflow = ref<AgentWorkflowTemplate | null>(null)
const runtimeStatus = ref<RuntimeStatus | null>(null)
const currentRun = ref<AgentRunRecord | null>(null)
const currentMaterial = ref<MaterialRecord | null>(null)
const logoUrl = '/slas-logo.svg'
const authLoading = ref(true)
const currentUser = ref<AuthUser | null>(null)
const loginBusy = ref(false)
const loginError = ref('')
const activeView = ref<WorkspaceView>('dashboard')
const sidebarOpen = ref(false)
const adminUsers = ref<AdminUserRecord[]>([])
const adminRoles = ref<RoleOption[]>([])
const adminKeyword = ref('')
const adminLoading = ref(false)
const userDialogVisible = ref(false)
const userSaving = ref(false)
const userDialogMode = ref<'create' | 'edit'>('create')
const userForm = ref({
  userId: '',
  username: '',
  password: '',
  realName: '',
  roles: ['STUDENT'],
  status: 1,
})
const passwordDialogVisible = ref(false)
const passwordSaving = ref(false)
const passwordForm = ref({ userId: '', username: '', password: '' })
const changePasswordDialogVisible = ref(false)
const changePasswordSaving = ref(false)
const changePasswordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const practiceSets = ref<PracticeSetRecord[]>([])
const currentPractice = ref<PracticeDetail | null>(null)
const practiceAnswers = ref<Record<string, string>>({})
const practiceResults = ref<Record<string, PracticeResult>>({})
const practiceAttempts = ref<PracticeAttemptRecord[]>([])
const wrongQuestions = ref<WrongQuestionRecord[]>([])
const masteryRecords = ref<KnowledgeMasteryRecord[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const title = ref('')
const subjectPreset = ref<SubjectPreset>('GENERAL')
const selectedCourseId = ref('')
const selectedChapterId = ref('')
const newCourseName = ref('')
const newCourseDescription = ref('')
const newCourseSubjectPreset = ref<SubjectPreset>('GENERAL')
const newChapterTitle = ref('')
const query = ref('')
const topic = ref('')
const difficulty = ref<QuestionDifficulty>('MEDIUM')
const count = ref(5)
const studentId = ref('demo-student')
const practiceCount = ref(5)
const questionTypes = ref<QuestionType[]>(['SINGLE_CHOICE', 'SHORT_ANSWER', 'FILL_BLANK'])
const loading = ref(false)
const practiceLoading = ref(false)
const selectedQuestionIds = ref<string[]>([])
const editDialogVisible = ref(false)
const editingQuestionId = ref('')
const editOptionsText = ref('')
const exportStatus = ref<QuestionStatus | ''>('')
const editQuestionForm = ref({
  prompt: '',
  answerText: '',
  analysisText: '',
  difficulty: 'MEDIUM' as QuestionDifficulty,
})

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

const roleDisplay: Record<string, { name: string; description: string }> = {
  ADMIN: { name: '系统管理员', description: '管理账号、角色与系统级配置，同时具备教师内容权限。' },
  TEACHER: { name: '教师', description: '维护课程、教材与题库，审核 AI 候选题并布置练习。' },
  STUDENT: { name: '学生', description: '完成练习、查看批改反馈，并跟踪错题与知识掌握度。' },
}

const indexedCount = computed(() => materials.value.filter((item) => item.status === 'INDEXED').length)
const pendingCount = computed(() => questions.value.filter((item) => item.status === 'PENDING_REVIEW').length)
const approvedCount = computed(() => questions.value.filter((item) => item.status === 'APPROVED').length)
const currentCourse = computed(() => courses.value.find((item) => item.courseId === selectedCourseId.value) ?? null)
const wrongCount = computed(() => wrongQuestions.value.reduce((sum, item) => sum + item.wrongCount, 0))
const averageMastery = computed(() => {
  if (masteryRecords.value.length === 0) return 0
  const total = masteryRecords.value.reduce((sum, item) => sum + item.mastery, 0)
  return Math.round((total / masteryRecords.value.length) * 100)
})
const recentAccuracy = computed(() => {
  if (practiceAttempts.value.length === 0) return 0
  const correct = practiceAttempts.value.filter((item) => item.correct).length
  return Math.round((correct / practiceAttempts.value.length) * 100)
})
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const userInitial = computed(() => {
  const name = currentUser.value?.realName || currentUser.value?.username || 'U'
  return name.slice(0, 2).toUpperCase()
})
const roleLabel = computed(() => {
  const roles = currentUser.value?.roles || []
  if (roles.includes('ADMIN')) return '系统管理员'
  if (roles.includes('TEACHER')) return '教师账号'
  if (roles.includes('STUDENT')) return '学生账号'
  return '学习者'
})
const isAdmin = computed(() => (currentUser.value?.roles || []).includes('ADMIN'))
const isTeacher = computed(() => (currentUser.value?.roles || []).includes('TEACHER'))
const canManageContent = computed(() => isAdmin.value || isTeacher.value)
const canEditStudentScope = computed(() => canManageContent.value)
const canCreatePractice = computed(() => canManageContent.value ? approvedCount.value > 0 : true)
const activeAdminCount = computed(() => adminUsers.value.filter((item) => item.status === 1).length)
onMounted(async () => {
  await initAuth()
})

async function initAuth() {
  authLoading.value = true
  loginError.value = ''
  try {
    if (getStoredToken()) {
      currentUser.value = await getCurrentUser()
      syncUserContext()
      await refreshAll()
    }
  } catch {
    clearStoredToken()
    currentUser.value = null
  } finally {
    authLoading.value = false
  }
}

async function login(credentials: { username: string; password: string }) {
  if (!credentials.username.trim() || !credentials.password) {
    loginError.value = '请输入用户名和密码'
    return
  }
  loginBusy.value = true
  loginError.value = ''
  try {
    const result = await loginWithPassword({
      username: credentials.username.trim(),
      password: credentials.password,
    })
    setStoredToken(result.token)
    currentUser.value = result.user
    activeView.value = 'dashboard'
    syncUserContext()
    ElMessage.success('登录成功')
    await refreshAll()
  } catch (error) {
    loginError.value = messageOf(error)
  } finally {
    loginBusy.value = false
  }
}

async function logout() {
  try {
    await logoutApi()
  } catch {
    // JWT 是无状态令牌，前端清理本地状态即可完成退出。
  }
  clearStoredToken()
  currentUser.value = null
  activeView.value = 'dashboard'
  sidebarOpen.value = false
  adminUsers.value = []
  adminRoles.value = []
  practiceSets.value = []
  currentPractice.value = null
  practiceAttempts.value = []
  wrongQuestions.value = []
  masteryRecords.value = []
  practiceAnswers.value = {}
  practiceResults.value = {}
  userDialogVisible.value = false
  passwordDialogVisible.value = false
  changePasswordDialogVisible.value = false
  ElMessage.success('已退出登录')
}

function setActiveView(view: WorkspaceView) {
  const managementViews: WorkspaceView[] = ['courses', 'materials', 'rag', 'questions', 'agents']
  if (managementViews.includes(view) && !canManageContent.value) return
  if (view === 'users' && !isAdmin.value) return
  activeView.value = view
  sidebarOpen.value = false
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleAccountCommand(command: 'password' | 'logout') {
  if (command === 'password') {
    openChangePassword()
    return
  }
  void logout()
}

function syncUserContext() {
  if (!currentUser.value) return
  if (!canEditStudentScope.value) {
    studentId.value = currentUser.value.userId
    return
  }
  studentId.value = studentId.value || 'demo-student'
}

function activeStudentId() {
  if (!canEditStudentScope.value && currentUser.value) {
    return currentUser.value.userId
  }
  return studentId.value.trim() || currentUser.value?.userId || 'demo-student'
}

async function loadAdminUsers() {
  if (!isAdmin.value) return
  adminLoading.value = true
  try {
    if (adminRoles.value.length === 0) {
      adminRoles.value = await listAdminRoles()
    }
    adminUsers.value = await listAdminUsers(adminKeyword.value.trim() || undefined)
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    adminLoading.value = false
  }
}

function openCreateUser() {
  userDialogMode.value = 'create'
  userForm.value = {
    userId: '',
    username: '',
    password: '',
    realName: '',
    roles: ['STUDENT'],
    status: 1,
  }
  userDialogVisible.value = true
}

function openEditUser(user: AdminUserRecord) {
  userDialogMode.value = 'edit'
  userForm.value = {
    userId: user.userId,
    username: user.username,
    password: '',
    realName: user.realName || '',
    roles: user.roles.length ? [...user.roles] : ['STUDENT'],
    status: user.status,
  }
  userDialogVisible.value = true
}

async function saveUser() {
  if (!userForm.value.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (userDialogMode.value === 'create' && !userForm.value.password) {
    ElMessage.warning('新建用户需要设置初始密码')
    return
  }
  const payload: UpsertUserPayload = {
    username: userForm.value.username.trim(),
    password: userForm.value.password || undefined,
    realName: userForm.value.realName.trim() || undefined,
    roles: userForm.value.roles.length ? userForm.value.roles : ['STUDENT'],
    status: userForm.value.status,
  }
  userSaving.value = true
  try {
    if (userDialogMode.value === 'create') {
      await createAdminUser(payload)
    } else {
      await updateAdminUser(userForm.value.userId, payload)
    }
    userDialogVisible.value = false
    ElMessage.success('账号已保存')
    await loadAdminUsers()
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    userSaving.value = false
  }
}

function openResetPassword(user: AdminUserRecord) {
  passwordForm.value = { userId: user.userId, username: user.username, password: '' }
  passwordDialogVisible.value = true
}

async function submitResetPassword() {
  if (passwordForm.value.password.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  passwordSaving.value = true
  try {
    await resetAdminPassword(passwordForm.value.userId, passwordForm.value.password)
    passwordDialogVisible.value = false
    ElMessage.success('密码已重置')
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    passwordSaving.value = false
  }
}

function openChangePassword() {
  changePasswordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  changePasswordDialogVisible.value = true
}

async function submitChangePassword() {
  if (!changePasswordForm.value.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (changePasswordForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (changePasswordForm.value.newPassword !== changePasswordForm.value.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changePasswordSaving.value = true
  try {
    await changePassword({
      oldPassword: changePasswordForm.value.oldPassword,
      newPassword: changePasswordForm.value.newPassword,
    })
    changePasswordDialogVisible.value = false
    ElMessage.success('密码已修改，请牢记新密码')
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    changePasswordSaving.value = false
  }
}

function roleName(roleCode: string) {
  return roleDisplay[roleCode]?.name || adminRoles.value.find((item) => item.roleCode === roleCode)?.roleName || roleCode
}

function roleDescription(roleCode: string, fallback: string) {
  return roleDisplay[roleCode]?.description || fallback
}

function roleTagType(roleCode: string) {
  if (roleCode === 'ADMIN') return 'danger'
  if (roleCode === 'TEACHER') return 'primary'
  if (roleCode === 'STUDENT') return 'success'
  return 'info'
}

async function refreshAll() {
  try {
    runtimeStatus.value = await getRuntimeStatus()
    if (canManageContent.value) {
      const [courseList, materialList, questionList, workflowTemplate, runs] = await Promise.all([
        listCourses(),
        listMaterials(),
        listQuestions(),
        getWorkflowTemplate(),
        listAgentRuns(),
      ])
      courses.value = courseList
      if (!selectedCourseId.value && courseList.length > 0) {
        selectedCourseId.value = courseList[0].courseId
      }
      materials.value = materialList
      questions.value = questionList
      workflow.value = workflowTemplate
      agentRuns.value = runs
      if (selectedCourseId.value) {
        await loadCourseDetail(selectedCourseId.value)
      } else {
        knowledgePoints.value = await listKnowledgePoints()
      }
      if (!currentMaterial.value && materialList.length > 0) {
        await selectMaterial(materialList[0])
      }
    } else {
      courses.value = []
      chapters.value = []
      knowledgePoints.value = []
      materials.value = []
      questions.value = []
      chunks.value = []
      hits.value = []
      workflow.value = null
      agentRuns.value = []
      currentMaterial.value = null
    }
    await loadLearningDashboard()
    if (isAdmin.value) {
      await loadAdminUsers()
      if (adminRoles.value.length === 0) {
        adminRoles.value = await listAdminRoles()
      }
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
    const uploaded = await uploadMaterial(selectedFile.value, title.value, subjectPreset.value, selectedCourseId.value || undefined, selectedChapterId.value || undefined)
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
    knowledgePoints.value = await listKnowledgePoints({ materialId: material.materialId })
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function addCourse() {
  if (!newCourseName.value.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  try {
    const course = await createCourse({
      name: newCourseName.value.trim(),
      subjectPreset: newCourseSubjectPreset.value,
      description: newCourseDescription.value,
    })
    newCourseName.value = ''
    newCourseDescription.value = ''
    selectedCourseId.value = course.courseId
    courses.value = await listCourses()
    await loadCourseDetail(course.courseId)
    ElMessage.success('课程已创建')
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function selectCourse(course: CourseRecord) {
  selectedCourseId.value = course.courseId
  subjectPreset.value = course.subjectPreset
  await loadCourseDetail(course.courseId)
}

async function loadCourseDetail(courseId: string) {
  chapters.value = await listChapters(courseId)
  if (selectedChapterId.value && !chapters.value.some((item) => item.chapterId === selectedChapterId.value)) {
    selectedChapterId.value = ''
  }
  knowledgePoints.value = await listKnowledgePoints({ courseId, chapterId: selectedChapterId.value || undefined })
}

async function addChapter() {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择课程')
    return
  }
  if (!newChapterTitle.value.trim()) {
    ElMessage.warning('请输入章节名称')
    return
  }
  try {
    const chapter = await createChapter(selectedCourseId.value, { title: newChapterTitle.value.trim() })
    newChapterTitle.value = ''
    selectedChapterId.value = chapter.chapterId
    await loadCourseDetail(selectedCourseId.value)
    ElMessage.success('章节已创建')
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function selectChapter(chapterId: string) {
  selectedChapterId.value = chapterId
  knowledgePoints.value = await listKnowledgePoints({ courseId: selectedCourseId.value, chapterId })
}

async function refreshCurrentKnowledgePoints() {
  if (!currentMaterial.value) {
    ElMessage.warning('请先选择教材')
    return
  }
  try {
    const points = await refreshKnowledgePoints(currentMaterial.value.materialId)
    knowledgePoints.value = points
    ElMessage.success(`已刷新 ${points.length} 个知识点`)
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

async function batchStatus(approved: boolean) {
  if (selectedQuestionIds.value.length === 0) {
    ElMessage.warning('请先选择题目')
    return
  }
  try {
    const updated = await batchUpdateQuestionStatus(selectedQuestionIds.value, approved)
    questions.value = await listQuestions()
    selectedQuestionIds.value = []
    ElMessage.success(`已批量${approved ? '通过' : '退回'} ${updated.length} 道题目`)
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

function openEditQuestion(question: QuestionRecord) {
  editingQuestionId.value = question.questionId
  editQuestionForm.value = {
    prompt: question.prompt,
    answerText: question.answerText,
    analysisText: question.analysisText,
    difficulty: question.difficulty,
  }
  editOptionsText.value = optionLines(question.options)
  editDialogVisible.value = true
}

async function saveQuestionEdit() {
  if (!editingQuestionId.value) return
  try {
    const payload = {
      prompt: editQuestionForm.value.prompt,
      answerText: editQuestionForm.value.answerText,
      analysisText: editQuestionForm.value.analysisText,
      difficulty: editQuestionForm.value.difficulty,
      options: parseOptionLines(editOptionsText.value),
    }
    await updateQuestion(editingQuestionId.value, payload)
    questions.value = await listQuestions()
    editDialogVisible.value = false
    ElMessage.success('题目已保存')
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function exportQuestionExcel() {
  try {
    const blob = await exportQuestions(currentMaterial.value?.materialId, exportStatus.value || undefined)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `smart-learning-questions-${Date.now()}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('题库 Excel 已导出')
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

function optionLines(options: QuestionOption[]) {
  return options.map((option) => `${option.label}|${option.text}|${option.correct}`).join('\n')
}

function parseOptionLines(value: string): QuestionOption[] {
  return value.split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const parts = line.split('|')
      const label = (parts[0] || String.fromCharCode(65 + index)).trim()
      const text = (parts[1] || parts[0] || '').trim()
      const correctText = (parts[2] || '').trim().toLowerCase()
      return { label, text, correct: ['true', '1', 'yes', 'y', '正确'].includes(correctText) }
    })
    .filter((option) => option.text)
}

async function loadLearningDashboard() {
  const learner = activeStudentId()
  try {
    const [sets, attempts, wrongs, mastery] = await Promise.all([
      listPracticeSets(learner),
      listPracticeAttempts({ studentId: learner }),
      listWrongQuestions(learner),
      listMastery(learner),
    ])
    practiceSets.value = sets
    practiceAttempts.value = attempts
    wrongQuestions.value = wrongs
    masteryRecords.value = mastery

    const activePracticeId = currentPractice.value?.practice.practiceId ?? sets[0]?.practiceId
    if (activePracticeId) {
      currentPractice.value = await getPracticeSet(activePracticeId)
      hydratePracticeAnswers(currentPractice.value)
    }
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function createLearningPractice() {
  practiceLoading.value = true
  try {
    const detail = await createPracticeSet({
      studentId: activeStudentId(),
      courseId: canManageContent.value ? selectedCourseId.value || undefined : undefined,
      chapterId: canManageContent.value ? selectedChapterId.value || undefined : undefined,
      materialId: canManageContent.value ? currentMaterial.value?.materialId : undefined,
      count: practiceCount.value,
    })
    currentPractice.value = detail
    hydratePracticeAnswers(detail)
    await loadLearningDashboard()
    ElMessage.success(`已生成 ${detail.questions.length} 道练习题`)
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    practiceLoading.value = false
  }
}

async function openPractice(set: PracticeSetRecord) {
  try {
    const detail = await getPracticeSet(set.practiceId)
    currentPractice.value = detail
    hydratePracticeAnswers(detail)
  } catch (error) {
    ElMessage.error(messageOf(error))
  }
}

async function submitPracticeQuestion(question: QuestionRecord) {
  if (!currentPractice.value) return
  const answerText = practiceAnswers.value[question.questionId]?.trim()
  if (!answerText) {
    ElMessage.warning('请先填写答案')
    return
  }
  practiceLoading.value = true
  try {
    const result = await submitPractice({
      practiceId: currentPractice.value.practice.practiceId,
      questionId: question.questionId,
      studentId: activeStudentId(),
      answerText,
    })
    practiceResults.value = { ...practiceResults.value, [question.questionId]: result }
    currentPractice.value = await getPracticeSet(currentPractice.value.practice.practiceId)
    hydratePracticeAnswers(currentPractice.value)
    const [sets, attempts, wrongs, mastery] = await Promise.all([
      listPracticeSets(activeStudentId()),
      listPracticeAttempts({ studentId: activeStudentId() }),
      listWrongQuestions(activeStudentId()),
      listMastery(activeStudentId()),
    ])
    practiceSets.value = sets
    practiceAttempts.value = attempts
    wrongQuestions.value = wrongs
    masteryRecords.value = mastery
    ElMessage.success(result.correct ? '回答正确，已更新掌握度' : '已记录到错题本，建议稍后复习')
  } catch (error) {
    ElMessage.error(messageOf(error))
  } finally {
    practiceLoading.value = false
  }
}

function hydratePracticeAnswers(detail: PracticeDetail) {
  const nextAnswers = { ...practiceAnswers.value }
  const nextResults = { ...practiceResults.value }
  for (const attempt of detail.attempts) {
    nextAnswers[attempt.questionId] = attempt.answerText
    nextResults[attempt.questionId] = {
      practiceId: attempt.practiceId,
      attemptId: attempt.attemptId,
      questionId: attempt.questionId,
      correct: attempt.correct,
      score: attempt.score,
      expectedAnswer: attempt.expectedAnswer,
      feedback: attempt.feedback,
      knowledgeNames: attempt.knowledgeNames,
    }
  }
  practiceAnswers.value = nextAnswers
  practiceResults.value = nextResults
}

function latestAttempt(questionId: string) {
  const attempts = currentPractice.value?.attempts
    .filter((item) => item.questionId === questionId)
    .sort((left, right) => right.submittedAt.localeCompare(left.submittedAt)) ?? []
  return attempts[0] ?? null
}

function resultFor(questionId: string) {
  const directResult = practiceResults.value[questionId]
  if (directResult) return directResult
  const attempt = latestAttempt(questionId)
  if (!attempt) return null
  return {
    practiceId: attempt.practiceId,
    attemptId: attempt.attemptId,
    questionId: attempt.questionId,
    correct: attempt.correct,
    score: attempt.score,
    expectedAnswer: attempt.expectedAnswer,
    feedback: attempt.feedback,
    knowledgeNames: attempt.knowledgeNames,
  }
}

function scorePercent(score: number) {
  return `${Math.round(score * 100)}%`
}

function materialName(materialId?: string | null) {
  if (!materialId) return '全部教材'
  return materials.value.find((item) => item.materialId === materialId)?.title ?? materialId
}

function scopeName(set: PracticeSetRecord) {
  const course = courses.value.find((item) => item.courseId === set.courseId)?.name
  const chapter = chapters.value.find((item) => item.chapterId === set.chapterId)?.title
  return [course, chapter, materialName(set.materialId)].filter(Boolean).join(' / ')
}

function masteryType(value: number) {
  if (value >= 0.8) return 'success'
  if (value >= 0.5) return 'warning'
  return 'exception'
}


async function openRun(run: AgentRunRecord) {
  currentRun.value = await getAgentRun(run.runId)
}

function messageOf(error: unknown) {
  return error instanceof Error ? error.message : String(error)
}
</script>

<template>
  <div v-if="authLoading" class="auth-loading">
    <img :src="logoUrl" alt="SLAS" />
    <div class="auth-loading__copy">
      <strong>正在进入学习空间</strong>
      <span>正在确认身份与服务状态...</span>
    </div>
    <span class="auth-loading__bar" aria-hidden="true"></span>
  </div>

  <LoginView v-else-if="!currentUser" :busy="loginBusy" :error="loginError" :logo-url="logoUrl" @submit="login" />

  <WorkspaceShell
    v-else
    v-model:sidebar-open="sidebarOpen"
    :active-view="activeView"
    :can-manage-content="canManageContent"
    :is-admin="isAdmin"
    :wrong-count="wrongCount"
    :pending-count="pendingCount"
    :runtime-status="runtimeStatus"
    :current-user="currentUser"
    :logo-url="logoUrl"
    :user-initial="userInitial"
    :role-label="roleLabel"
    :current-material-title="currentMaterial?.title"
    @navigate="setActiveView"
    @refresh="refreshAll"
    @account-command="handleAccountCommand"
  >

        <DashboardOverview
          v-if="activeView === 'dashboard'"
          :can-manage-content="canManageContent"
          :role-label="roleLabel"
          :display-name="currentUser.realName || currentUser.username"
          :greeting="greeting"
          :materials-count="materials.length"
          :indexed-count="indexedCount"
          :approved-count="approvedCount"
          :pending-count="pendingCount"
          :practice-set-count="practiceSets.length"
          :practice-attempt-count="practiceAttempts.length"
          :recent-accuracy="recentAccuracy"
          :average-mastery="averageMastery"
          :wrong-count="wrongCount"
          :mastery-records="masteryRecords"
          :hits-count="hits.length"
          :current-course-name="currentCourse?.name"
          :current-material-title="currentMaterial?.title"
          :current-practice="currentPractice"
          :runtime-status="runtimeStatus"
          @navigate="setActiveView"
        />



    <section v-if="canManageContent && activeView === 'courses'" id="courses" class="workspace-grid view-section">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><DataAnalysis /></el-icon>课程与章节</div></template>
        <label class="form-field">
          <span>课程名称</span>
          <el-input v-model="newCourseName" placeholder="例如：大学英语、数据结构" />
        </label>
        <label class="form-field">
          <span>学科类型</span>
          <el-select v-model="newCourseSubjectPreset">
            <el-option v-for="item in subjectOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </label>
        <label class="form-field">
          <span>课程说明</span>
          <el-input v-model="newCourseDescription" type="textarea" :rows="2" placeholder="选填" />
        </label>
        <el-button type="primary" @click="addCourse">创建课程</el-button>
        <div class="course-list">
          <button v-for="course in courses" :key="course.courseId" :class="['material-item', { active: course.courseId === selectedCourseId }]" @click="selectCourse(course)">
            <strong>{{ course.name }}</strong>
            <span>{{ course.subjectPreset }} · {{ course.description || '暂无说明' }}</span>
          </button>
          <div v-if="courses.length === 0" class="empty-note">暂无课程，填写上方信息后创建第一门课程。</div>
        </div>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><Document /></el-icon>章节与知识点</div></template>
        <div class="chapter-create">
          <el-input v-model="newChapterTitle" placeholder="新章节名称，例如：Unit 1 / 第一章" />
          <el-button type="primary" :disabled="!selectedCourseId" @click="addChapter">新增章节</el-button>
        </div>
        <div class="chapter-list">
          <button :class="['chapter-pill', { active: selectedChapterId === '' }]" @click="selectedChapterId = ''; selectedCourseId && loadCourseDetail(selectedCourseId)">全部章节</button>
          <button v-for="chapter in chapters" :key="chapter.chapterId" :class="['chapter-pill', { active: chapter.chapterId === selectedChapterId }]" @click="selectChapter(chapter.chapterId)">
            {{ chapter.chapterOrder }}. {{ chapter.title }}
          </button>
        </div>
        <div class="knowledge-head">
          <strong>候选知识点 {{ knowledgePoints.length }}</strong>
          <el-button size="small" :disabled="!currentMaterial" @click="refreshCurrentKnowledgePoints">刷新当前教材知识点</el-button>
        </div>
        <div class="knowledge-list">
          <article v-for="point in knowledgePoints.slice(0, 12)" :key="point.knowledgePointId" class="knowledge-card">
            <strong>{{ point.name }}</strong>
            <small>weight {{ point.weight.toFixed(2) }}</small>
            <p>{{ point.sourceSnippet }}</p>
          </article>
          <div v-if="knowledgePoints.length === 0" class="empty-note">当前范围暂无知识点，请先选择并解析教材。</div>
        </div>
      </el-card>
    </section>

    <section v-if="canManageContent && activeView === 'materials'" id="materials" class="workspace-grid view-section">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><UploadFilled /></el-icon>教材上传与解析</div></template>
        <input ref="fileInput" type="file" class="hidden-input" accept=".pdf,.docx,.txt,.md,.xlsx,.xls" @change="onFileChange" />
        <div class="upload-box" @click="chooseFile">
          <el-icon><Document /></el-icon>
          <strong>{{ selectedFile?.name || '选择教材文件' }}</strong>
          <span>支持 PDF、DOCX、TXT、Markdown、Excel</span>
        </div>
        <label class="form-field">
          <span>教材标题</span>
          <el-input v-model="title" placeholder="默认使用文件名" />
        </label>
        <label class="form-field">
          <span>学科类型</span>
          <el-select v-model="subjectPreset">
            <el-option v-for="item in subjectOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </label>
        <el-button type="primary" :disabled="!selectedFile" :loading="loading" @click="uploadAndParse">上传并解析入库</el-button>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><DataAnalysis /></el-icon>教材列表</div></template>
        <div class="material-list">
          <button v-for="item in materials" :key="item.materialId" :class="['material-item', { active: item.materialId === currentMaterial?.materialId }]" @click="selectMaterial(item)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.subjectPreset }} · {{ item.status }} · {{ item.chunkCount }} chunks</span>
          </button>
          <div v-if="materials.length === 0" class="empty-note">知识库还是空的，请从左侧上传一份教材。</div>
        </div>
      </el-card>
    </section>

    <section v-if="canManageContent && activeView === 'rag'" id="rag" class="workspace-grid view-section">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><Search /></el-icon>向量检索验证</div></template>
        <label class="form-field">
          <span>检索主题</span>
          <el-input v-model="query" type="textarea" :rows="3" placeholder="例如：报价邮件、被动语态、二叉树遍历" />
        </label>
        <div :class="['task-prerequisite', { ready: currentMaterial }]">
          <div>
            <small>当前检索范围</small>
            <strong>{{ currentMaterial?.title || '请先选择一份已解析教材' }}</strong>
          </div>
          <button type="button" @click="setActiveView('materials')">{{ currentMaterial ? '更换教材' : '选择教材' }}</button>
        </div>
        <el-button type="primary" :disabled="!currentMaterial" :loading="loading" @click="runRetrieve">检索教材证据</el-button>
        <div class="hit-list">
          <article v-for="hit in hits" :key="hit.chunk.chunkId" class="hit-card">
            <div class="hit-score">score {{ hit.score.toFixed(3) }}</div>
            <p>{{ hit.chunk.text }}</p>
          </article>
          <div v-if="hits.length === 0" class="empty-note">输入检索主题并执行检索后，相关教材证据会显示在这里。</div>
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
          <div v-if="chunks.length === 0" class="empty-note">选择已解析教材后可查看切片内容与关键词。</div>
        </div>
      </el-card>
    </section>

    <section v-if="canManageContent && activeView === 'questions'" id="questions" class="workspace-grid wide view-section">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><MagicStick /></el-icon>生成习题</div></template>
        <div :class="['task-prerequisite', { ready: currentMaterial }]">
          <div>
            <small>题目来源</small>
            <strong>{{ currentMaterial?.title || '尚未选择教材，暂时不能生成' }}</strong>
          </div>
          <button type="button" @click="setActiveView('materials')">{{ currentMaterial ? '更换来源' : '选择教材' }}</button>
        </div>
        <label class="form-field">
          <span>出题主题</span>
          <el-input v-model="topic" placeholder="例如：外贸报价、英语时态、软件测试基础" />
        </label>
        <div class="form-field">
          <span>题型组合</span>
          <el-checkbox-group v-model="questionTypes">
            <el-checkbox-button v-for="item in questionTypeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-checkbox-button>
          </el-checkbox-group>
        </div>
        <div class="inline-form question-generate-form">
          <el-select v-model="difficulty"><el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" /></el-select>
          <el-input-number v-model="count" :min="1" :max="20" />
          <el-button type="primary" :disabled="!currentMaterial" :loading="loading" @click="runGenerate">启动多智能体生成</el-button>
        </div>
      </el-card>

      <el-card class="panel question-panel" shadow="never">
        <template #header>
          <div class="panel-title question-toolbar">
            <span>题库审核</span>
            <div class="toolbar-actions">
              <el-select v-model="exportStatus" placeholder="导出状态" clearable size="small">
                <el-option label="待审核" value="PENDING_REVIEW" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已退回" value="REJECTED" />
              </el-select>
              <el-button size="small" @click="exportQuestionExcel">导出 Excel</el-button>
              <el-button size="small" type="success" :disabled="selectedQuestionIds.length === 0" @click="batchStatus(true)">批量通过</el-button>
              <el-button size="small" type="danger" plain :disabled="selectedQuestionIds.length === 0" @click="batchStatus(false)">批量退回</el-button>
            </div>
          </div>
        </template>
        <el-checkbox-group v-model="selectedQuestionIds" class="question-select-group">
          <article v-for="question in questions" :key="question.questionId" class="question-card">
          <div class="question-head">
            <el-checkbox :label="question.questionId">选择</el-checkbox>
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
            <el-button size="small" @click="openEditQuestion(question)">编辑</el-button>
            <el-button size="small" type="success" @click="updateStatus(question, true)">通过</el-button>
            <el-button size="small" type="danger" plain @click="updateStatus(question, false)">退回</el-button>
          </div>
          </article>
          <div v-if="questions.length === 0" class="empty-note">题库暂无内容，请先选择教材并启动生成任务。</div>
        </el-checkbox-group>
      </el-card>
    </section>

    <section v-if="activeView === 'learning'" id="learning" class="workspace-grid wide view-section">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><DataAnalysis /></el-icon>学习练习闭环</div></template>
        <label class="form-field">
          <span>学习者 ID</span>
          <el-input v-model="studentId" :disabled="!canEditStudentScope" placeholder="例如：demo-student" />
        </label>
        <div class="inline-form practice-scope-form">
          <el-input-number v-model="practiceCount" :min="1" :max="20" />
          <el-button type="primary" :disabled="!canCreatePractice" :loading="practiceLoading" @click="createLearningPractice">{{ canManageContent ? '生成练习任务' : '开始新练习' }}</el-button>
          <el-button :loading="practiceLoading" @click="loadLearningDashboard">刷新画像</el-button>
        </div>
        <div v-if="canManageContent && approvedCount === 0" class="task-prerequisite">
          <div><small>组卷前置条件</small><strong>题库中还没有审核通过的题目</strong></div>
          <button type="button" @click="setActiveView('questions')">前往审核</button>
        </div>
        <div class="practice-scope">
          <strong>当前范围</strong>
          <span>{{ currentCourse?.name || '全部课程' }} / {{ selectedChapterId ? chapters.find((item) => item.chapterId === selectedChapterId)?.title : '全部章节' }} / {{ currentMaterial?.title || '全部教材' }}</span>
          <small>系统会优先从已审核通过题目中组卷；如果没有选择教材，则按当前课程和章节筛选。</small>
        </div>
        <div class="practice-stats">
          <div><strong>{{ practiceSets.length }}</strong><span>练习任务</span></div>
          <div><strong>{{ practiceAttempts.length }}</strong><span>提交次数</span></div>
          <div><strong>{{ wrongCount }}</strong><span>累计错题</span></div>
          <div><strong>{{ averageMastery }}%</strong><span>平均掌握</span></div>
        </div>
        <div class="practice-set-list">
          <button v-for="set in practiceSets" :key="set.practiceId" :class="['practice-set-item', { active: set.practiceId === currentPractice?.practice.practiceId }]" @click="openPractice(set)">
            <strong>{{ set.title }}</strong>
            <span>{{ set.status }} · {{ set.questionIds.length }} 题 · {{ scopeName(set) }}</span>
          </button>
          <div v-if="practiceSets.length === 0" class="empty-note">当前学习者暂无练习任务。</div>
        </div>
      </el-card>

      <el-card class="panel practice-panel" shadow="never">
        <template #header>
          <div class="panel-title question-toolbar">
            <span>学生答题与自动批改</span>
            <el-tag v-if="currentPractice" type="success">{{ currentPractice.practice.status }}</el-tag>
          </div>
        </template>
        <div v-if="!currentPractice" class="empty-note">
          先在左侧生成练习任务，或选择已有任务查看题目、提交答案和查看反馈。
        </div>
        <article v-for="question in currentPractice?.questions" :key="question.questionId" class="practice-question-card">
          <div class="question-head">
            <el-tag>{{ question.type }}</el-tag>
            <el-tag type="warning">{{ question.difficulty }}</el-tag>
            <el-tag v-if="resultFor(question.questionId)" :type="resultFor(question.questionId)?.correct ? 'success' : 'danger'">
              {{ resultFor(question.questionId)?.correct ? '已掌握' : '需复习' }} · {{ scorePercent(resultFor(question.questionId)?.score || 0) }}
            </el-tag>
          </div>
          <p class="question-prompt">{{ question.prompt }}</p>
          <div v-if="question.options.length" class="option-list">
            <span v-for="option in question.options" :key="option.label">{{ option.label }}. {{ option.text }}</span>
          </div>
          <el-input v-model="practiceAnswers[question.questionId]" type="textarea" :rows="2" placeholder="输入你的答案，选择题可填写 A/B/C/D" />
          <div v-if="resultFor(question.questionId)" class="practice-feedback">
            <strong>{{ resultFor(question.questionId)?.feedback }}</strong>
            <span>参考答案：{{ resultFor(question.questionId)?.expectedAnswer }}</span>
            <small v-if="resultFor(question.questionId)?.knowledgeNames.length">关联知识点：{{ resultFor(question.questionId)?.knowledgeNames.join(' / ') }}</small>
          </div>
          <div class="card-actions">
            <el-button size="small" type="primary" :loading="practiceLoading" @click="submitPracticeQuestion(question)">提交并批改</el-button>
          </div>
        </article>
      </el-card>
    </section>

    <section v-if="activeView === 'progress'" class="workspace-grid view-section">
      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><Document /></el-icon>错题本</div></template>
        <div v-if="wrongQuestions.length === 0" class="empty-note">当前学习者暂无错题。提交练习后，错误记录会自动归档到这里。</div>
        <article v-for="item in wrongQuestions.slice(0, 8)" :key="item.questionId" class="wrong-card">
          <div class="wrong-head">
            <strong>错误 {{ item.wrongCount }} 次</strong>
            <span>{{ materialName(item.materialId) }}</span>
          </div>
          <p>{{ item.prompt }}</p>
          <small>你的答案：{{ item.lastAnswer || '未填写' }}</small>
          <small>参考答案：{{ item.expectedAnswer }}</small>
          <small>反馈：{{ item.lastFeedback }}</small>
        </article>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><DataAnalysis /></el-icon>知识掌握度</div></template>
        <div v-if="masteryRecords.length === 0" class="empty-note">暂无掌握度数据。每次提交练习都会按知识点累计正确率。</div>
        <article v-for="item in masteryRecords.slice(0, 10)" :key="`${item.materialId}-${item.knowledgeName}`" class="mastery-card">
          <div class="mastery-title">
            <strong>{{ item.knowledgeName }}</strong>
            <span>{{ item.correctAttempts }}/{{ item.totalAttempts }}</span>
          </div>
          <el-progress :percentage="Math.round(item.mastery * 100)" :status="masteryType(item.mastery)" />
          <small>{{ materialName(item.materialId) }}</small>
        </article>
      </el-card>
    </section>

    <section v-if="isAdmin && activeView === 'users'" id="users" class="workspace-grid wide view-section">
      <el-card class="panel admin-panel" shadow="never">
        <template #header>
          <div class="panel-title question-toolbar">
            <span>账号与角色管理</span>
            <el-button size="small" type="primary" @click="openCreateUser">新建账号</el-button>
          </div>
        </template>
        <div class="admin-summary">
          <article><small>账号总数</small><strong>{{ adminUsers.length }}</strong></article>
          <article><small>启用账号</small><strong>{{ activeAdminCount }}</strong></article>
          <article><small>角色类型</small><strong>{{ adminRoles.length }}</strong></article>
        </div>
        <div class="admin-toolbar">
          <el-input v-model="adminKeyword" clearable placeholder="搜索用户名、姓名或角色" @keyup.enter="loadAdminUsers" @clear="loadAdminUsers" />
          <el-button :loading="adminLoading" @click="loadAdminUsers">刷新</el-button>
        </div>
        <div class="admin-user-list" v-loading="adminLoading">
          <article v-for="user in adminUsers" :key="user.userId" class="admin-user-card">
            <div class="admin-user-main">
              <span class="avatar">{{ (user.realName || user.username).slice(0, 2).toUpperCase() }}</span>
              <div>
                <strong>{{ user.realName || user.username }}</strong>
                <small>{{ user.username }} · {{ user.lastLoginAt ? `最近登录 ${new Date(user.lastLoginAt).toLocaleString()}` : '尚未登录' }}</small>
              </div>
            </div>
            <div class="role-chip-list">
              <el-tag v-for="role in user.roles" :key="role" :type="roleTagType(role)" effect="light">{{ roleName(role) }}</el-tag>
              <el-tag :type="user.status === 1 ? 'success' : 'info'" effect="light">{{ user.status === 1 ? '启用' : '停用' }}</el-tag>
            </div>
            <div class="card-actions">
              <el-button size="small" @click="openEditUser(user)">编辑</el-button>
              <el-button size="small" type="warning" plain @click="openResetPassword(user)">重置密码</el-button>
            </div>
          </article>
        </div>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header><div class="panel-title"><el-icon><DataAnalysis /></el-icon>权限说明</div></template>
        <div class="role-explain-list">
          <article v-for="role in adminRoles" :key="role.roleCode" class="role-explain-card">
            <el-tag :type="roleTagType(role.roleCode)" effect="light">{{ role.roleCode }}</el-tag>
            <div>
              <strong>{{ roleName(role.roleCode) }}</strong>
              <p>{{ roleDescription(role.roleCode, role.description) }}</p>
            </div>
          </article>
        </div>
        <div class="empty-note">
          当前版本已经具备 JWT 登录、管理员账号维护和个人改密能力。后续可以在此基础上继续细分“教师可维护题库、学生只可练习”的接口级权限。
        </div>
      </el-card>
    </section>

    <AgentRunsView
      v-if="canManageContent && activeView === 'agents'"
      :workflow="workflow"
      :runs="agentRuns"
      :current-run="currentRun"
      @select-run="openRun"
    />


    <el-dialog v-model="userDialogVisible" :title="userDialogMode === 'create' ? '新建账号' : '编辑账号'" width="720px" class="account-dialog">
      <el-form label-position="top">
        <div class="admin-form-grid">
          <el-form-item label="用户名">
            <el-input v-model="userForm.username" placeholder="3-32 位字母、数字、下划线、点或短横线" />
          </el-form-item>
          <el-form-item label="姓名/昵称">
            <el-input v-model="userForm.realName" placeholder="例如：张老师 / Demo Student" />
          </el-form-item>
          <el-form-item v-if="userDialogMode === 'create'" label="初始密码">
            <el-input v-model="userForm.password" type="password" show-password placeholder="至少 6 位" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="userForm.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item class="admin-form-full" label="角色">
            <el-checkbox-group v-model="userForm.roles">
              <el-checkbox-button v-for="role in adminRoles" :key="role.roleCode" :label="role.roleCode">{{ roleName(role.roleCode) }}</el-checkbox-button>
            </el-checkbox-group>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSaving" @click="saveUser">保存账号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="460px">
      <el-form label-position="top">
        <el-form-item label="目标账号">
          <el-input v-model="passwordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="至少 6 位" @keyup.enter="submitResetPassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="submitResetPassword">确认重置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="changePasswordDialogVisible" title="修改当前账号密码" width="500px">
      <el-form label-position="top">
        <el-form-item label="当前密码">
          <el-input v-model="changePasswordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="changePasswordForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="changePasswordForm.confirmPassword" type="password" show-password @keyup.enter="submitChangePassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changePasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="changePasswordSaving" @click="submitChangePassword">保存新密码</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑题目" width="720px" class="question-edit-dialog">
      <el-form label-position="top">
        <el-form-item label="题干">
          <el-input v-model="editQuestionForm.prompt" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="选项（每行格式：A|选项内容|true，非选择题可留空）">
          <el-input v-model="editOptionsText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="editQuestionForm.answerText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="editQuestionForm.analysisText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="editQuestionForm.difficulty">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestionEdit">保存</el-button>
      </template>
    </el-dialog>
  </WorkspaceShell>
</template>
