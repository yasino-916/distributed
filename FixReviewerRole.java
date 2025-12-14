import java.sql.*;

public class FixReviewerRole {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distributed_quiz";
        String user = "root";
        String pass = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, pass);

            // Delete reviewer from admins table
            String deleteSql = "DELETE FROM admins WHERE username = 'reviewer'";
            Statement stmt = conn.createStatement();
            int deleted = stmt.executeUpdate(deleteSql);

            System.out.println("Deleted " + deleted + " reviewer entry(ies) from admins table.");

            // Verify reviewer exists in reviewers table
            String checkSql = "SELECT * FROM reviewers WHERE username = 'reviewer'";
            ResultSet rs = stmt.executeQuery(checkSql);

            if (rs.next()) {
                System.out.println("✓ Reviewer found in reviewers table:");
                System.out.println("  ID: " + rs.getInt("id"));
                System.out.println("  Username: " + rs.getString("username"));
                System.out.println("  Full Name: " + rs.getString("full_name"));
            } else {
                System.out.println("✗ Reviewer NOT found in reviewers table! Need to add it.");
            }

            conn.close();
            System.out.println("\nFix complete! Please restart the server and try logging in again.");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
