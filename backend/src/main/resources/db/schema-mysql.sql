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
