export type WorkspaceView =
  | 'dashboard'
  | 'courses'
  | 'materials'
  | 'rag'
  | 'questions'
  | 'learning'
  | 'progress'
  | 'users'
  | 'agents'

export interface WorkspaceMeta {
  eyebrow: string
  title: string
  description: string
}

export function getWorkspaceMeta(view: WorkspaceView, canManageContent: boolean): WorkspaceMeta {
  const views: Record<WorkspaceView, WorkspaceMeta> = {
    dashboard: {
      eyebrow: canManageContent ? 'TEACHING STUDIO' : 'LEARNING STUDIO',
      title: canManageContent ? '教学工作概览' : '我的学习概览',
      description: canManageContent
        ? '从教材入库到练习反馈，掌握当前教学内容的处理进度。'
        : '查看练习进度、答题表现和需要优先复习的知识点。',
    },
    courses: {
      eyebrow: '01 / CONTENT SCOPE',
      title: '课程与知识点',
      description: '先建立课程与章节范围，再让教材和题目各归其位。',
    },
    materials: {
      eyebrow: '02 / SOURCE LIBRARY',
      title: '教材知识库',
      description: '上传可信资料，完成解析与索引，为后续生成保留来源依据。',
    },
    rag: {
      eyebrow: '03 / EVIDENCE CHECK',
      title: '检索验证',
      description: '用真实问题检查教材片段是否可被准确召回。',
    },
    questions: {
      eyebrow: '04 / QUESTION STUDIO',
      title: '智能题库',
      description: '生成候选题、核对来源证据，并由教师完成最终审核。',
    },
    learning: {
      eyebrow: canManageContent ? '05 / PRACTICE DELIVERY' : 'MY PRACTICE',
      title: canManageContent ? '练习任务' : '我的练习',
      description: canManageContent
        ? '按学习者和内容范围组卷，并查看自动批改结果。'
        : '完成分配给你的练习，并根据即时反馈订正答案。',
    },
    progress: {
      eyebrow: '06 / LEARNING SIGNALS',
      title: '学习画像',
      description: '结合错题与知识掌握度，定位下一步最值得复习的内容。',
    },
    users: {
      eyebrow: 'ADMIN / ACCESS',
      title: '账号与权限',
      description: '维护系统账号、角色边界与启用状态。',
    },
    agents: {
      eyebrow: 'TRACE / AGENT RUNS',
      title: '智能体运行',
      description: '检查角色分工、真实执行步骤、工具调用与失败信息。',
    },
  }
  return views[view]
}
