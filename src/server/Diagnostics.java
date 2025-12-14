package server;

import java.sql.*;

public class Diagnostics {
    public static void main(String[] args) {
        System.out.println("Running Diagnostics...");
        String URL = "jdbc:mysql://localhost:3306/distributed_quiz";
        String USER = "root";
        String PASS = "";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Connected to Database.");

            // Check 'students' columns
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "students", null);
            System.out.println("Columns in 'students' table:");
            boolean hasGender = false;
            while (columns.next()) {
                String colName = columns.getString("COLUMN_NAME");
                String colType = columns.getString("TYPE_NAME");
                System.out.println(" - " + colName + " (" + colType + ")");
                if ("gender".equalsIgnoreCase(colName)) {
                    hasGender = true;
                }
            }

            if (hasGender) {
                System.out.println("SUCCESS: 'gender' column found.");
            } else {
                System.out.println("FAILURE: 'gender' column MISSING.");
            }

            // Check specifically for user 'dbu1500931'
            String targetUser = "dbu1500931";
            PreparedStatement psCheck = conn.prepareStatement("SELECT * FROM students WHERE username = ?");
            psCheck.setString(1, targetUser);
            ResultSet rsCheck = psCheck.executeQuery();

            if (rsCheck.next()) {
                System.out.println("User '" + targetUser + "' ALREADY EXISTS in database.");
                System.out.println("  ID: " + rsCheck.getInt("id") + " | Name: " + rsCheck.getString("full_name"));
            } else {
                System.out.println("User '" + targetUser + "' NOT found. Attempting to insert...");
                try {
                    PreparedStatement psIns = conn.prepareStatement(
                            "INSERT INTO students (username, password, full_name, department, gender) VALUES (?, 'pass123', 'Test DBU User', 'Eng', 'Male')");
                    psIns.setString(1, targetUser);
                    int r = psIns.executeUpdate();
                    System.out.println("INSERT SUCCESS: Added '" + targetUser + "' -> Rows: " + r);
                } catch (Exception e) {
                    System.out.println("INSERT FAILED for '" + targetUser + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // List Top 5 Students (verify order)
            System.out.println("Top 5 Students (ORDER BY id DESC):");
            Statement stmt = conn.createStatement();
            ResultSet rsTop = stmt
                    .executeQuery("SELECT id, username, full_name, gender FROM students ORDER BY id DESC LIMIT 5");
            while (rsTop.next()) {
                System.out.println(" - ID: " + rsTop.getInt("id") + " | " + rsTop.getString("username") + " | "
                        + rsTop.getString("full_name") + " | Gen: " + rsTop.getString("gender"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
