-- =========================================================
-- Seed Script (PostgreSQL) for the BIGINT/JPA-friendly schema
-- Creates:
--   - 1 quiz
--   - 4 questions (SINGLE + MULTI)
--   - options (with is_correct on server side)
--   - 1 demo user + identities (google + linkedin)
--   - 1 demo submitted attempt + selected answers
-- =========================================================

-- -------------------------
-- 1) Create a quiz
-- -------------------------
WITH ins_quiz AS (
  INSERT INTO quiz (title, description, is_active)
  VALUES ('Java + Spring + React Basics', 'Seed quiz for SINGLE and MULTI questions', true)
  RETURNING id
),

-- -------------------------
-- 2) Create questions (ordered by question_no)
-- -------------------------
ins_q AS (
  INSERT INTO question (quiz_id, question_no, type, text, points, is_required)
  SELECT id, 1, 'SINGLE', 'What is Spring Boot?', 1.00, true FROM ins_quiz
  UNION ALL
  SELECT id, 2, 'SINGLE', 'React is a...', 1.00, true FROM ins_quiz
  UNION ALL
  SELECT id, 3, 'MULTI',  'Which of these are JVM languages?', 2.00, true FROM ins_quiz
  UNION ALL
  SELECT id, 4, 'MULTI',  'Select all HTTP methods that are idempotent (commonly defined).', 2.00, true FROM ins_quiz
  RETURNING id, question_no, quiz_id
),

-- -------------------------
-- 3) Create options for each question
-- -------------------------
ins_opt AS (
  INSERT INTO question_option (question_id, option_no, text, is_correct)
  SELECT q.id, o.option_no, o.text, o.is_correct
  FROM ins_q q
  JOIN (VALUES
    -- Q1: What is Spring Boot? (SINGLE)
    (1, 1, 'A Database', false),
    (1, 2, 'A Java Framework', true),
    (1, 3, 'A Browser', false),

    -- Q2: React is a... (SINGLE)
    (2, 1, 'Library', true),
    (2, 2, 'Framework', false),
    (2, 3, 'Operating System', false),

    -- Q3: JVM languages (MULTI)
    (3, 1, 'Java', true),
    (3, 2, 'Kotlin', true),
    (3, 3, 'Python', false),
    (3, 4, 'Scala', true),

    -- Q4: Idempotent methods (MULTI)
    (4, 1, 'GET', true),
    (4, 2, 'POST', false),
    (4, 3, 'PUT', true),
    (4, 4, 'DELETE', true),
    (4, 5, 'PATCH', false)
  ) AS o(question_no, option_no, text, is_correct)
    ON o.question_no = q.question_no
  RETURNING 1
)
SELECT 'Seed: quiz/questions/options created' AS status;


-- -------------------------
-- 4) Create a demo user (idempotent insert)
-- -------------------------
INSERT INTO app_user (external_id, full_name, email)
SELECT 'STUDENT_123', 'Demo Student', 'demo.student@example.com'
WHERE NOT EXISTS (
  SELECT 1 FROM app_user WHERE external_id = 'STUDENT_123'
);

-- -------------------------
-- 5) Create demo social identities (google + linkedin)
-- provider_subject values are illustrative; in real life they come from OIDC "sub"
-- -------------------------
WITH u AS (
  SELECT id AS user_id FROM app_user WHERE external_id = 'STUDENT_123'
)
INSERT INTO user_identity (user_id, provider, provider_subject, email, email_verified, display_name, picture_url, last_login_at)
SELECT u.user_id, x.provider, x.provider_subject, x.email, x.email_verified, x.display_name, x.picture_url, now()
FROM u
JOIN (VALUES
  ('google',   'google-sub-1234567890',  'demo.student@example.com', true,  'Demo Student', 'https://example.com/avatar-google.png'),
  ('linkedin', 'linkedin-sub-abcdef123', 'demo.student@example.com', true,  'Demo Student', 'https://example.com/avatar-linkedin.png')
) AS x(provider, provider_subject, email, email_verified, display_name, picture_url)
  ON true
ON CONFLICT (provider, provider_subject) DO NOTHING;


-- -------------------------
-- 6) Create a demo submitted attempt + selected answers
--    - Q1 correct (option 2)
--    - Q2 correct (option 1)
--    - Q3 correct selections (Java, Kotlin, Scala)
--    - Q4 correct selections (GET, PUT, DELETE)
-- Total points: 1 + 1 + 2 + 2 = 6
-- Score here is illustrative (e.g., 6.00)
-- -------------------------
WITH
u AS (
  SELECT id AS user_id FROM app_user WHERE external_id = 'STUDENT_123'
),
qz AS (
  SELECT id AS quiz_id FROM quiz WHERE title = 'Java + Spring + React Basics' ORDER BY id DESC LIMIT 1
),
total_pts AS (
  SELECT COALESCE(SUM(points), 0)::numeric(10,2) AS total_points
  FROM question
  WHERE quiz_id = (SELECT quiz_id FROM qz)
),
ins_attempt AS (
  INSERT INTO attempt (quiz_id, user_id, status, started_at, submitted_at, score, total_points)
  SELECT qz.quiz_id,
         u.user_id,
         'SUBMITTED',
         now() - interval '5 minutes',
         now(),
         6.00,
         (SELECT total_points FROM total_pts)
  FROM u, qz
  RETURNING id AS attempt_id, quiz_id
),
qs AS (
  SELECT question_no, id AS question_id
  FROM question
  WHERE quiz_id = (SELECT quiz_id FROM ins_attempt)
),
opts AS (
  SELECT q.question_no, qo.option_no, qo.id AS option_id, q.id AS question_id
  FROM question q
  JOIN question_option qo ON qo.question_id = q.id
  WHERE q.quiz_id = (SELECT quiz_id FROM ins_attempt)
),
chosen AS (
  -- Define chosen options (by question_no + option_no)
  SELECT * FROM (VALUES
    (1, 2), -- Q1 option 2
    (2, 1), -- Q2 option 1
    (3, 1), -- Q3 options 1,2,4 (Java,Kotlin,Scala)
    (3, 2),
    (3, 4),
    (4, 1), -- Q4 options 1,3,4 (GET,PUT,DELETE)
    (4, 3),
    (4, 4)
  ) AS c(question_no, option_no)
)
INSERT INTO attempt_answer (attempt_id, question_id, option_id)
SELECT a.attempt_id, o.question_id, o.option_id
FROM ins_attempt a
JOIN chosen c ON true
JOIN opts o
  ON o.question_no = c.question_no
 AND o.option_no   = c.option_no;

SELECT 'Seed: demo user/identities/attempt created' AS status;
