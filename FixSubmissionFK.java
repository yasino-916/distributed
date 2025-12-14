import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixSubmissionFK {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distributed_quiz";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("Connected to database.");

            // 1. Drop the incorrect Foreign Key
            try {
                System.out.println("Attempting to drop incorrect FK 'student_submissions_ibfk_1'...");
                stmt.executeUpdate("ALTER TABLE student_submissions DROP FOREIGN KEY student_submissions_ibfk_1");
                System.out.println("Dropped 'student_submissions_ibfk_1'.");
            } catch (Exception e) {
                System.out.println(
                        "Could not drop 'student_submissions_ibfk_1' (maybe it doesn't exist?): " + e.getMessage());
            }

            // 1.1 Drop the incorrect Foreign Key (Alternative name if auto-generated
            // differently)
            try {
                System.out.println("Attempting to drop potential FK 'student_submissions_ibfk_2'...");
                stmt.executeUpdate("ALTER TABLE student_submissions DROP FOREIGN KEY student_submissions_ibfk_2");
                System.out.println("Dropped 'student_submissions_ibfk_2'.");
            } catch (Exception e) {
                // Ignore
            }

            // 2. Add the Correct Foreign Key referencing students(id)
            try {
                System.out.println("Adding correct FK referencing 'students(id)'...");
                stmt.executeUpdate(
                        "ALTER TABLE student_submissions ADD CONSTRAINT fk_student_submissions_students FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE");
                System.out.println("Success! Foreign Key fixed.");
            } catch (Exception e) {
                System.err.println("Error adding FK: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
