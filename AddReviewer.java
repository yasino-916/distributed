import java.sql.*;

public class AddReviewer {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distributed_quiz";
        String user = "root";
        String pass = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, pass);

            String sql = "INSERT INTO admins (username, password, full_name) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "reviewer");
            pstmt.setString(2, "pass123");
            pstmt.setString(3, "Exam Reviewer");

            int result = pstmt.executeUpdate();

            if (result > 0) {
                System.out.println("SUCCESS: Reviewer account added successfully!");
                System.out.println("Username: reviewer");
                System.out.println("Password: pass123");
            } else {
                System.out.println("FAILED: Could not add reviewer account.");
            }

            conn.close();
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("INFO: Reviewer account already exists.");
                System.out.println("Username: reviewer");
                System.out.println("Password: pass123");
            } else {
                System.err.println("ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
