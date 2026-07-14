CREATE TABLE IF NOT EXISTS app_user (
  user_id VARCHAR(64) PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(128),
  roles_json TEXT,
  status INT DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  last_login_at DATETIME,
  INDEX idx_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO app_user (user_id, username, password_hash, real_name, roles_json, status, created_at, updated_at)
VALUES
  ('demo-admin', 'admin', 'pbkdf2$120000$c2xhcy1hZG1pbi0yMDI2$8afB8/d+AVte0kFW4n+s6Q4xmT83Pq60QfXyAaOXrXU=', 'System Admin', '["ADMIN","TEACHER"]', 1, NOW(), NOW()),
  ('demo-teacher', 'teacher', 'pbkdf2$120000$c2xhcy10ZWFjaGVyLTIwMjY=$11uJ/P/Mc18abzvgJdf0DugCcby3bRfRYjrRcBQMJHA=', 'Demo Teacher', '["TEACHER"]', 1, NOW(), NOW()),
  ('demo-student', 'student', 'pbkdf2$120000$c2xhcy1zdHVkZW50LTIwMjY=$sfUsjgUJCNxyvGkC+6EEhO9LEaE9gGmS8Qy69w69kik=', 'Demo Student', '["STUDENT"]', 1, NOW(), NOW());

CREATE TABLE IF NOT EXISTS material (
  material_id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  subject_preset VARCHAR(64) NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(128),
  storage_path VARCHAR(1024),
  status VARCHAR(32) NOT NULL,
  chunk_count INT DEFAULT 0,
  error_message TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS material_chunk (
  chunk_id VARCHAR(64) PRIMARY KEY,
  material_id VARCHAR(64) NOT NULL,
  chapter_id VARCHAR(64),
  chapter_title VARCHAR(255),
  chunk_index INT NOT NULL,
  page_no INT,
  source_label VARCHAR(255),
  text LONGTEXT NOT NULL,
  keywords_json TEXT,
  created_at DATETIME NOT NULL,
  INDEX idx_material_chunk_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS question (
  question_id VARCHAR(64) PRIMARY KEY,
  task_id VARCHAR(64),
  material_id VARCHAR(64),
  type VARCHAR(64) NOT NULL,
  difficulty VARCHAR(32) NOT NULL,
  subject_preset VARCHAR(64) NOT NULL,
  prompt TEXT NOT NULL,
  options_json TEXT,
  answer_text TEXT NOT NULL,
  analysis_text TEXT,
  status VARCHAR(32) NOT NULL,
  source_refs_json TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_question_task (task_id),
  INDEX idx_question_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS generation_task (
  task_id VARCHAR(64) PRIMARY KEY,
  material_id VARCHAR(64) NOT NULL,
  subject_preset VARCHAR(64) NOT NULL,
  topic VARCHAR(255),
  question_types_json TEXT,
  difficulty VARCHAR(32),
  requested_count INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  agent_run_id VARCHAR(64),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_generation_task_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_run (
  run_id VARCHAR(64) PRIMARY KEY,
  task_id VARCHAR(64),
  objective TEXT,
  status VARCHAR(32) NOT NULL,
  steps_json LONGTEXT,
  tool_calls_json LONGTEXT,
  final_answer TEXT,
  workflow_mode VARCHAR(32) NOT NULL DEFAULT 'RAG_MULTI_AGENT',
  failed_role VARCHAR(64),
  error_summary VARCHAR(500),
  started_at DATETIME NOT NULL,
  finished_at DATETIME,
  metrics_json TEXT,
  created_at DATETIME NOT NULL,
  INDEX idx_agent_run_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS learning_course (
  course_id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  subject_preset VARCHAR(64) NOT NULL,
  description TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_chapter (
  chapter_id VARCHAR(64) PRIMARY KEY,
  course_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  chapter_order INT DEFAULT 0,
  description TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_course_chapter_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS material_course_binding (
  material_id VARCHAR(64) PRIMARY KEY,
  course_id VARCHAR(64) NOT NULL,
  chapter_id VARCHAR(64),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_material_course_binding_course (course_id),
  INDEX idx_material_course_binding_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_point (
  knowledge_point_id VARCHAR(64) PRIMARY KEY,
  course_id VARCHAR(64),
  chapter_id VARCHAR(64),
  material_id VARCHAR(64) NOT NULL,
  chunk_id VARCHAR(64),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  source_snippet TEXT,
  weight DOUBLE DEFAULT 0,
  created_at DATETIME NOT NULL,
  INDEX idx_knowledge_point_course (course_id),
  INDEX idx_knowledge_point_chapter (chapter_id),
  INDEX idx_knowledge_point_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS learning_practice_set (
  practice_id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  student_id VARCHAR(64) NOT NULL,
  course_id VARCHAR(64),
  chapter_id VARCHAR(64),
  material_id VARCHAR(64),
  question_ids_json TEXT,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_practice_set_student (student_id),
  INDEX idx_practice_set_course (course_id),
  INDEX idx_practice_set_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS learning_practice_attempt (
  attempt_id VARCHAR(64) PRIMARY KEY,
  practice_id VARCHAR(64),
  student_id VARCHAR(64) NOT NULL,
  question_id VARCHAR(64) NOT NULL,
  answer_text TEXT,
  correct BOOLEAN DEFAULT FALSE,
  score DOUBLE DEFAULT 0,
  expected_answer TEXT,
  feedback TEXT,
  knowledge_names_json TEXT,
  submitted_at DATETIME NOT NULL,
  INDEX idx_practice_attempt_practice (practice_id),
  INDEX idx_practice_attempt_student (student_id),
  INDEX idx_practice_attempt_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wrong_question (
  student_id VARCHAR(64) NOT NULL,
  question_id VARCHAR(64) NOT NULL,
  material_id VARCHAR(64),
  prompt TEXT,
  expected_answer TEXT,
  last_answer TEXT,
  last_feedback TEXT,
  wrong_count INT DEFAULT 1,
  last_submitted_at DATETIME NOT NULL,
  PRIMARY KEY (student_id, question_id),
  INDEX idx_wrong_question_student (student_id),
  INDEX idx_wrong_question_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_mastery (
  student_id VARCHAR(64) NOT NULL,
  course_id VARCHAR(64),
  chapter_id VARCHAR(64),
  material_id VARCHAR(64) NOT NULL,
  knowledge_name VARCHAR(255) NOT NULL,
  total_attempts INT DEFAULT 0,
  correct_attempts INT DEFAULT 0,
  mastery DOUBLE DEFAULT 0,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (student_id, material_id, knowledge_name),
  INDEX idx_knowledge_mastery_student (student_id),
  INDEX idx_knowledge_mastery_course (course_id),
  INDEX idx_knowledge_mastery_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
