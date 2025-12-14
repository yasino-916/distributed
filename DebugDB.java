import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DebugDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distributed_quiz";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("=== DEBUG: Subjects (Exams) ===");
            ResultSet rsSubj = stmt.executeQuery("SELECT id, name, access_code FROM subjects");
            while (rsSubj.next()) {
                System.out.printf("ID: %d | Name: %s | Code: %s%n", rsSubj.getInt("id"), rsSubj.getString("name"),
                        rsSubj.getString("access_code"));
            }

            System.out.println("\n=== DEBUG: Students (First 5) ===");
            ResultSet rsStud = stmt.executeQuery("SELECT id, username, full_name, score FROM students LIMIT 5");
            while (rsStud.next()) {
                System.out.printf("ID: %d | User: %s | Name: %s | Score: %d%n", rsStud.getInt("id"),
                        rsStud.getString("username"), rsStud.getString("full_name"), rsStud.getInt("score"));
            }

            System.out.println("\n=== DEBUG: Student Submissions (ALL) ===");
            ResultSet rsSubs = stmt.executeQuery("SELECT id, student_id, subject_id, score FROM student_submissions");
            boolean hasSubs = false;
            while (rsSubs.next()) {
                hasSubs = true;
                System.out.printf("SubID: %d | StudentID: %d | SubjectID: %d | Score: %d%n", rsSubs.getInt("id"),
                        rsSubs.getInt("student_id"), rsSubs.getInt("subject_id"), rsSubs.getInt("score"));
            }
            if (!hasSubs)
                System.out.println("No submissions found.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
