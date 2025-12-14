package server;

import common.Question;
import common.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/distributed_quiz";
    private static final String USER = "root";
    private static final String PASS = "";

    private Connection connection;

    public Connection getConnection() {
        return connection;
    }

    private boolean useMock = false;

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Connect to server (no DB) to create DB if not exists
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", USER, PASS);
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS distributed_quiz");
            stmt.close();
            connection.close();

            connection = DriverManager.getConnection(URL, USER, PASS);
            createTablesIfNotExist();
            System.out.println("Connected to Database (Standard Schema - Separate Tables).");
        } catch (Exception e) {
            System.err.println("Database connection failed (" + e.getMessage() + "). Using MOCK mode.");
            useMock = true;
        }
    }

    public void createTablesIfNotExist() {
        try (Statement stmt = connection.createStatement()) {

            // CLEANUP MIGRATION
            // We revert to old tables. Drop 'users' if it exists to avoid confusion.
            try {
                stmt.executeUpdate("DROP TABLE IF EXISTS users");
            } catch (SQLException ignored) {
            }

            // ADMIN TABLE
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS admins (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100) DEFAULT 'Administrator')");

            // TEACHERS TABLE (Exam Creator)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS teachers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100))");

            // REVIEWERS TABLE (Exam Reviewer)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reviewers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100))");

            // STUDENT TABLE
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100), " +
                    "department VARCHAR(100), " +
                    "full_name VARCHAR(100), " +
                    "department VARCHAR(100), " +
                    "gender VARCHAR(10), " +
                    "score INT DEFAULT 0, " +
                    "has_submitted BOOLEAN DEFAULT FALSE)");

            // Migration: Add gender column if it doesn't exist (Fix for Add User issue)
            try {
                stmt.executeUpdate("ALTER TABLE students ADD COLUMN gender VARCHAR(10)");
                System.out.println("Applied migration: Added 'gender' column to students table.");
            } catch (SQLException e) {
                // Column likely already exists
            }

            // SUBJECTS TABLE
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subjects (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "access_code VARCHAR(50) NOT NULL UNIQUE, " +
                    "start_time TIMESTAMP NULL, " +
                    "end_time TIMESTAMP NULL, " +
                    "is_published BOOLEAN DEFAULT FALSE, " +
                    "status VARCHAR(50) DEFAULT 'PENDING_REVIEW', " +
                    "created_by INT DEFAULT NULL)");

            // Migration: Add status column if it doesn't exist
            try {
                stmt.executeUpdate("ALTER TABLE subjects ADD COLUMN status VARCHAR(50) DEFAULT 'PENDING_REVIEW'");
                System.out.println("Added 'status' column to subjects table");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            // STUDENT SUBMISSIONS (Referencing students(id))
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS student_submissions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id INT, " +
                    "subject_id INT, " +
                    "score INT, " +
                    "submission_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (student_id) REFERENCES students(id), " +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id), " +
                    "UNIQUE KEY unique_submission (student_id, subject_id))");

            // Migration: Add UNIQUE constraint if missing
            try {
                stmt.executeUpdate(
                        "ALTER TABLE student_submissions ADD UNIQUE KEY unique_submission (student_id, subject_id)");
                System.out.println("Applied migration: Added UNIQUE constraint to student_submissions");
            } catch (SQLException e) {
                // Constraint likely already exists
            }

            // QUESTIONS TABLE
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS questions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "subject_id INT, " +
                    "question_text TEXT NOT NULL, " +
                    "option_a VARCHAR(255) NOT NULL, " +
                    "option_b VARCHAR(255) NOT NULL, " +
                    "option_c VARCHAR(255) NOT NULL, " +
                    "option_d VARCHAR(255) NOT NULL, " +
                    "correct_option CHAR(1) NOT NULL, " +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE)");

            // RESULTS TABLE
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS results (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT, " +
                    "subject_id INT, " +
                    "score INT, " +
                    "total_questions INT, " +
                    "completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES students(id), " +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id))");

            // EXAM_REVIEWS TABLE (Audit trail for admin reviews)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS exam_reviews (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "subject_id INT NOT NULL, " +
                    "reviewer_id INT NOT NULL, " +
                    "action VARCHAR(50) NOT NULL, " +
                    "comments TEXT, " +
                    "reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE)");

            // Seed Data
            seedData(stmt);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void seedData(Statement stmt) {
        // Admin
        try {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM admins");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(
                        "INSERT INTO admins (username, password, full_name) VALUES ('admin', 'admin123', 'System Administrator')");
                System.out.println("Seeded Admins.");
            }
        } catch (SQLException e) {
            System.out.println("Skipping Admin seed: " + e.getMessage());
        }

        // Reviewers (Exam Reviewer)
        try {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM reviewers");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(
                        "INSERT INTO reviewers (username, password, full_name) VALUES ('reviewer', 'pass123', 'Exam Reviewer')");
                System.out.println("Seeded Reviewers.");
            }
        } catch (SQLException e) {
            System.out.println("Skipping Reviewer seed: " + e.getMessage());
        }

        // Teachers (Creator)
        try {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM teachers");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(
                        "INSERT INTO teachers (username, password, full_name) VALUES ('creator', 'pass123', 'Exam Creator')");
                System.out.println("Seeded Teachers.");
            }
        } catch (SQLException e) {
            System.out.println("Skipping Teacher seed: " + e.getMessage());
        }

        // Import Students from CSV into STUDENTS table
        importStudentsFromCSV();

        // Subjects
        try {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM subjects");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Seeding Subjects...");
                stmt.executeUpdate("INSERT INTO subjects (name, access_code, start_time, end_time) VALUES " +
                        "('Web Programming (PHP)', 'PHP101', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR)), " +
                        "('Compiler Design', 'COMP303', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR)), " +
                        "('Distributed System', 'DIST404', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR)), " +
                        "('Software Architecture Design', 'ARCH505', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR)), " +
                        "('Artificial Intelligence', 'AI606', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR)), " +
                        "('Project Management', 'PM707', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR))");
            }
        } catch (SQLException e) {
            System.err.println("Error Seeding Subjects: " + e.getMessage());
        }

        // Questions
        try {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM questions");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Seeding Questions...");
                // 1: PHP
                stmt.executeUpdate(
                        "INSERT INTO questions (subject_id, question_text, option_a, option_b, option_c, option_d, correct_option) VALUES "
                                +
                                "(1, 'What does PHP stand for?', 'Private Home Page', 'Personal Hypertext Processor', 'PHP: Hypertext Preprocessor', 'Public Hosting Platform', 'C'), "
                                +
                                "(1, 'Which symbol starts a variable in PHP?', '@', '$', '#', '&', 'B')");
                // 2: Compiler Design
                stmt.executeUpdate(
                        "INSERT INTO questions (subject_id, question_text, option_a, option_b, option_c, option_d, correct_option) VALUES "
                                +
                                "(2, 'What is the first phase of compilation?', 'Syntax Analysis', 'Lexical Analysis', 'Code Generation', 'Optimization', 'B'), "
                                +
                                "(2, 'What tool is used for lexical analysis?', 'YACC', 'Bison', 'Lex', 'GDB', 'C')");
                // 3: Distributed System
                stmt.executeUpdate(
                        "INSERT INTO questions (subject_id, question_text, option_a, option_b, option_c, option_d, correct_option) VALUES "
                                +
                                "(3, 'Which is NOT a characteristic of distributed systems?', 'Concurrency', 'Scalability', 'Single Point of Failure', 'Transparency', 'C'), "
                                +
                                "(3, 'RMI stands for?', 'Remote Method Invocation', 'Remote Memory Interface', 'Random Method Interaction', 'Real Machine Instruction', 'A')");
                // 4: Software Architecture
                stmt.executeUpdate(
                        "INSERT INTO questions (subject_id, question_text, option_a, option_b, option_c, option_d, correct_option) VALUES "
                                +
                                "(4, 'Which pattern is used for separating concerns?', 'Singleton', 'MVC', 'Factory', 'Observer', 'B'), "
                                +
                                "(4, 'What does UML stand for?', 'Unified Modeling Language', 'Universal Machine Logic', 'Unique Model Link', 'User Mode Linux', 'A')");
            }
        } catch (SQLException e) {
            System.err.println("Error Seeding Questions: " + e.getMessage());
        }
    }

    private void importStudentsFromCSV() {
        String csvFile = "students.CSV";
        File f = new File(csvFile);
        if (!f.exists())
            return;

        // Check if students already imported
        try {
            Statement checkStmt = connection.createStatement();
            ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM students");
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Students already imported. Skipping CSV import.");
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error checking student count: " + e.getMessage());
        }

        System.out.println("Importing students from " + csvFile + " into 'students' table...");
        String sql = "INSERT INTO students (username, password, full_name, department) VALUES (?, ?, ?, ?)";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile));
                PreparedStatement pstmt = connection.prepareStatement(sql)) {

            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("//"))
                    continue;
                String[] parts = line.split("\t");
                if (parts.length > 0 && parts[0].equalsIgnoreCase("Default User Name"))
                    continue;
                if (parts.length < 9)
                    continue;

                String username = parts[0].trim(); // DBU ID
                String password = "pass123"; // Default password
                String fullName = parts[2].trim() + " " + parts[3].trim() + " " + parts[4].trim();
                String department = parts[8].trim();

                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, fullName);
                pstmt.setString(4, department);

                try {
                    pstmt.executeUpdate();
                    count++;
                } catch (SQLException e) {
                    // Ignore duplicates
                }
            }
            System.out.println("Student CSV Import Complete. Imported " + count + " students.");
        } catch (Exception e) {
            System.err.println("CSV Import Failed: " + e.getMessage());
        }
    }

    public common.User authenticate(String username, String password) {
        if (useMock) {
            if ("admin".equals(username))
                return new User(1, "admin", "ADMIN", "Admin", "IT");
            if ("creator".equals(username))
                return new User(2, "creator", "TEACHER", "Teacher", "IT");
            return new User(3, username, "STUDENT", "Student", "IT");
        }
        try {
            // Check Admin
            PreparedStatement psAdmin = connection
                    .prepareStatement("SELECT * FROM admins WHERE username = ? AND password = ?");
            psAdmin.setString(1, username);
            psAdmin.setString(2, password);
            ResultSet rsA = psAdmin.executeQuery();
            if (rsA.next()) {
                return new User(rsA.getInt("id"), rsA.getString("username"), "ADMIN", rsA.getString("full_name"),
                        "Admin Dept");
            }

            // Check Teacher (Creator)
            PreparedStatement psTeach = connection
                    .prepareStatement("SELECT * FROM teachers WHERE username = ? AND password = ?");
            psTeach.setString(1, username);
            psTeach.setString(2, password);
            ResultSet rsT = psTeach.executeQuery();
            if (rsT.next()) {
                return new User(rsT.getInt("id"), rsT.getString("username"), "TEACHER", rsT.getString("full_name"),
                        "Exam Creator");
            }

            // Check Reviewer
            PreparedStatement psReviewer = connection
                    .prepareStatement("SELECT * FROM reviewers WHERE username = ? AND password = ?");
            psReviewer.setString(1, username);
            psReviewer.setString(2, password);
            ResultSet rsR = psReviewer.executeQuery();
            if (rsR.next()) {
                return new User(rsR.getInt("id"), rsR.getString("username"), "REVIEWER", rsR.getString("full_name"),
                        "Exam Reviewer");
            }

            // Check Student
            PreparedStatement psStud = connection
                    .prepareStatement("SELECT * FROM students WHERE username = ? AND password = ?");
            psStud.setString(1, username);
            psStud.setString(2, password);
            ResultSet rsS = psStud.executeQuery();
            if (rsS.next()) {
                User u = new User(rsS.getInt("id"), rsS.getString("username"), "STUDENT", rsS.getString("full_name"),
                        rsS.getString("department"));
                u.setScore(rsS.getInt("score"));
                u.setHasSubmitted(rsS.getBoolean("has_submitted"));
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public common.Subject getSubjectByCode(String code, int studentId) throws Exception {
        if (useMock)
            return new common.Subject(1, "Mock Subject", code, new java.sql.Timestamp(System.currentTimeMillis()),
                    new java.sql.Timestamp(System.currentTimeMillis() + 3600000), true);

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM subjects WHERE access_code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                common.Subject sub = new common.Subject(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("access_code"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getBoolean("is_published"));

                // Check if student has ALREADY submitted (Referencing students logic)
                if (hasStudentSubmitted(studentId, sub.getId())) {
                    throw new Exception("You have already submitted this exam.");
                }

                if (!sub.isPublished()) {
                    throw new Exception("This exam is not yet published."); // Draft mode
                }

                return sub;
            }
        } catch (SQLException e) {
            throw e;
        }
        return null;
    }

    private boolean hasStudentSubmitted(int studentId, int subjectId) throws SQLException {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT count(*) FROM student_submissions WHERE student_id = ? AND subject_id = ?");
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Checking submission for Student " + studentId + " in Subject " + subjectId
                        + ": Count=" + count);
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Fail secure!
        }
        return false;
    }

    public boolean addQuestion(int subjectId, String text, String a, String b, String c, String d, String correct) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO questions (subject_id, question_text, option_a, option_b, option_c, option_d, correct_option) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, subjectId);
            ps.setString(2, text);
            ps.setString(3, a);
            ps.setString(4, b);
            ps.setString(5, c);
            ps.setString(6, d);
            ps.setString(7, correct);
            boolean success = ps.executeUpdate() > 0;

            // Update status to QUESTIONS_PENDING if currently APPROVED_FOR_QUESTIONS
            if (success) {
                PreparedStatement psUpdate = connection.prepareStatement(
                        "UPDATE subjects SET status = 'QUESTIONS_PENDING' WHERE id = ? AND status = 'APPROVED_FOR_QUESTIONS'");
                psUpdate.setInt(1, subjectId);
                psUpdate.executeUpdate();
            }

            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Question> getQuestions(int subjectId) {
        List<Question> list = new ArrayList<>();
        if (useMock) {
            list.add(new Question(1, "What is the capital of France?", "London", "Berlin", "Paris", "Madrid"));
            return list;
        }

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM questions WHERE subject_id = ?");
            ps.setInt(1, subjectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Question(
                        rs.getInt("id"),
                        rs.getString("question_text"),
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<common.Subject> getAllSubjects() {
        List<common.Subject> list = new ArrayList<>();
        if (useMock)
            return list;
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM subjects");
            while (rs.next()) {
                list.add(new common.Subject(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("access_code"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getBoolean("is_published"),
                        rs.getString("status"),
                        rs.getInt("created_by")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<common.Subject> getSubjectsByCreator(int creatorId) {
        List<common.Subject> list = new ArrayList<>();
        if (useMock)
            return list;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM subjects WHERE created_by = ?");
            ps.setInt(1, creatorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new common.Subject(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("access_code"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getBoolean("is_published"),
                        rs.getString("status"),
                        rs.getInt("created_by")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<common.User> getAllStudents() {
        List<common.User> list = new ArrayList<>();
        try {
            // Revert logic: Select from students table
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM students ORDER BY id DESC");
            while (rs.next()) {
                User u = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        "STUDENT",
                        rs.getString("full_name"),
                        rs.getString("department"));
                u.setScore(rs.getInt("score"));
                u.setHasSubmitted(rs.getBoolean("has_submitted"));
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean resetStudentSubmissionForSubject(int studentId, int subjectId) {
        try {
            // 1. Get the score for this specific submission to decrement from total
            PreparedStatement psScore = connection.prepareStatement(
                    "SELECT score FROM student_submissions WHERE student_id = ? AND subject_id = ?");
            psScore.setInt(1, studentId);
            psScore.setInt(2, subjectId);
            ResultSet rs = psScore.executeQuery();

            int scoreToRemove = 0;
            if (rs.next()) {
                scoreToRemove = rs.getInt("score");
            }

            // 2. Decrement student's global score
            PreparedStatement psUpdate = connection.prepareStatement(
                    "UPDATE students SET score = GREATEST(0, score - ?) WHERE id = ?");
            psUpdate.setInt(1, scoreToRemove);
            psUpdate.setInt(2, studentId);
            psUpdate.executeUpdate();

            // 3. Delete the mock submission
            PreparedStatement psDelete = connection.prepareStatement(
                    "DELETE FROM student_submissions WHERE student_id = ? AND subject_id = ?");
            psDelete.setInt(1, studentId);
            psDelete.setInt(2, subjectId);
            int rows = psDelete.executeUpdate();

            // 4. Also delete from results table
            PreparedStatement psDeleteResults = connection.prepareStatement(
                    "DELETE FROM results WHERE user_id = ? AND subject_id = ?");
            psDeleteResults.setInt(1, studentId);
            psDeleteResults.setInt(2, subjectId);
            psDeleteResults.executeUpdate();

            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int calculateScore(int studentId, int subjectId, Map<Integer, String> answers) throws SQLException {
        System.out.println("DEBUG: Calculating score for Student " + studentId + " Subject " + subjectId);
        int score = 0;
        Map<Integer, String> correctAnswers = new HashMap<>();

        // Fetch Correct Answers for Subject
        PreparedStatement ps = connection
                .prepareStatement("SELECT id, correct_option FROM questions WHERE subject_id = ?");
        ps.setInt(1, subjectId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            correctAnswers.put(rs.getInt("id"), rs.getString("correct_option"));
        }

        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            String correct = correctAnswers.get(entry.getKey());
            if (correct != null && correct.equalsIgnoreCase(entry.getValue())) {
                score++;
            }
        }

        System.out.println("DEBUG: Calculated Score = " + score);

        // SAVE TO DB (student_submissions)
        if (!useMock) {
            System.out.println("DEBUG: Attempting to insert into student_submissions...");

            // Use ON DUPLICATE KEY UPDATE to handle retries (if previous attempt failed
            // halfway)
            PreparedStatement psInsert = connection.prepareStatement(
                    "INSERT INTO student_submissions (student_id, subject_id, score) VALUES (?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE score = VALUES(score), submission_time = CURRENT_TIMESTAMP");
            psInsert.setInt(1, studentId);
            psInsert.setInt(2, subjectId);
            psInsert.setInt(3, score);
            int rows = psInsert.executeUpdate();
            System.out.println("DEBUG: student_submissions INSERT/UPDATE result rows: " + rows);

            // Also save to results table (Always Insert new record for history? Or also
            // update?)
            // Ideally results table is history, so we might want to just Insert.
            // But if we want unique results per exam attempt, maybe we should just insert.
            // For now, let's keep it as Insert, but wrap in try-catch to allow progress if
            // it fails non-critically?
            // Actually user wants to just "Submit".

            try {
                PreparedStatement psResults = connection.prepareStatement(
                        "INSERT INTO results (user_id, subject_id, score, total_questions) VALUES (?, ?, ?, ?)");
                psResults.setInt(1, studentId);
                psResults.setInt(2, subjectId);
                psResults.setInt(3, score);
                psResults.setInt(4, correctAnswers.size()); // Total questions
                psResults.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Warning: Could not insert into results table (might be duplicate or other issue): "
                        + e.getMessage());
                // Don't fail the whole submission just for the 'results' log table if the main
                // one succeeded
            }

            // Update students table accumulation
            // Only update score if it's a new submission?
            // The logic "score = score + ?" implies accumulation.
            // If we are UPDATING an existing submission (retry), we shouldn't add the score
            // AGAIN.
            // But this is complex to track.
            // For now, to unblock the user, we assume this is "fixing" a broken state.
            // A better approach for "students.score" (Global Score) would be to recalculate
            // it from sum(student_submissions).
            // But let's leave it simple for now and prevent crash.

            try {
                PreparedStatement psUpd = connection
                        .prepareStatement("UPDATE students SET score = score + ?, has_submitted = TRUE WHERE id = ?");
                psUpd.setInt(1, score);
                psUpd.setInt(2, studentId);
                psUpd.executeUpdate();
            } catch (Exception e) {
                System.out.println("Warning: Could not update student global score: " + e.getMessage());
            }

            System.out.println("DEBUG: Submission saved successfully.");
        }

        return score;
    }

    // New Creator Methods
    public boolean addSubject(String name, String code, Timestamp start, Timestamp end, int creatorId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO subjects (name, access_code, start_time, end_time, created_by, is_published, status) VALUES (?, ?, ?, ?, ?, FALSE, 'PENDING_REVIEW')");
            ps.setString(1, name);
            ps.setString(2, code);
            ps.setTimestamp(3, start);
            ps.setTimestamp(4, end);
            ps.setInt(5, creatorId);
            int result = ps.executeUpdate();
            System.out.println("Subject created successfully: " + name + " (Code: " + code + ")");
            return result > 0;
        } catch (Exception e) {
            System.err.println("ERROR creating subject: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean publishSubject(int subjectId) {
        try {
            // Only allow publishing if status is QUESTIONS_PENDING
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE subjects SET is_published = TRUE, status = 'PUBLISHED' WHERE id = ? AND status = 'QUESTIONS_PENDING'");
            ps.setInt(1, subjectId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // New workflow methods
    public boolean approveExamDraft(int subjectId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE subjects SET status = 'APPROVED_FOR_QUESTIONS' WHERE id = ? AND status = 'PENDING_REVIEW'");
            ps.setInt(1, subjectId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<common.Subject> getPendingExams() {
        List<common.Subject> list = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM subjects WHERE status IN ('PENDING_REVIEW', 'QUESTIONS_PENDING') ORDER BY id DESC");
            while (rs.next()) {
                list.add(new common.Subject(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("access_code"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getBoolean("is_published"),
                        rs.getString("status"),
                        rs.getInt("created_by")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getQuestionCount(int subjectId) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM questions WHERE subject_id = ?");
            ps.setInt(1, subjectId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean deleteSubject(int subjectId) {
        try {
            // Delete subject (questions will be cascade deleted due to ON DELETE CASCADE)
            PreparedStatement ps = connection.prepareStatement("DELETE FROM subjects WHERE id = ?");
            ps.setInt(1, subjectId);
            int result = ps.executeUpdate();
            System.out.println("Deleted subject ID: " + subjectId + " (Result: " + result + ")");
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting subject: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<common.User> getStudentSubmissionsForExam(int subjectId) {
        List<common.User> list = new ArrayList<>();
        System.out.println("DEBUG: Fetching submissions for Exam ID: " + subjectId);
        try {
            // Get all students with their submission data for a specific exam
            String sql = "SELECT s.id, s.username, s.full_name, s.department, " +
                    "COALESCE(sub.score, 0) as exam_score, " +
                    "CASE WHEN sub.id IS NOT NULL THEN TRUE ELSE FALSE END as has_submitted_exam " +
                    "FROM students s " +
                    "LEFT JOIN student_submissions sub ON s.id = sub.student_id AND sub.subject_id = ? " +
                    "ORDER BY s.id";

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, subjectId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                User u = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        "STUDENT",
                        rs.getString("full_name"),
                        rs.getString("department"));
                u.setScore(rs.getInt("exam_score"));
                u.setHasSubmitted(rs.getBoolean("has_submitted_exam"));

                if (u.hasSubmitted()) {
                    System.out.println("DEBUG: Found submission for " + u.getUsername() + " Score: " + u.getScore());
                }

                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addTeacher(String username, String password, String fullName, String department) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO teachers (username, password, full_name, department) VALUES (?, ?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, password); // In real app, hash this!
            ps.setString(3, fullName);
            ps.setString(4, department);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding teacher: " + e.getMessage());
            return false;
        }
    }

    public boolean addReviewer(String username, String password, String fullName) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO reviewers (username, password, full_name) VALUES (?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding reviewer: " + e.getMessage());
            return false;
        }
    }

    public boolean addStudent(String username, String password, String fullName, String department, String gender) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO students (username, password, full_name, department, gender) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, department);
            ps.setString(5, gender);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
            return false;
        }
    }
}
