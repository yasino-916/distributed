import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixSchema {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distributed_quiz";
        String user = "root";
        String password = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();

            System.out.println("Applying Schema Fixes...");

            // 1. Fix Teachers Table (Department)
            try {
                stmt.executeUpdate("ALTER TABLE teachers ADD COLUMN department VARCHAR(100)");
                System.out.println("SUCCESS: Added 'department' to teachers.");
            } catch (Exception e) {
                System.out.println("Teachers Fix: " + e.getMessage());
            }

            // 2. Fix Students Table (Gender)
            try {
                stmt.executeUpdate("ALTER TABLE students ADD COLUMN gender VARCHAR(20)");
                System.out.println("SUCCESS: Added 'gender' to students.");
            } catch (Exception e) {
                System.out.println("Students Fix: " + e.getMessage());
            }

            // 3. Fix Reviewers Table incase it wasn't created
            try {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reviewers (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(50) NOT NULL UNIQUE, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "full_name VARCHAR(100))");
                System.out.println("SUCCESS: Ensured reviewers table exists.");
            } catch (Exception e) {
                System.out.println("Reviewers Fix: " + e.getMessage());
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
