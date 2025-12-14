import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixResultsTable {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distributed_quiz";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("Connected to database.");

            // Add subject_id column to results table
            try {
                System.out.println("Attempting to add 'subject_id' column to 'results' table...");
                stmt.executeUpdate("ALTER TABLE results ADD COLUMN subject_id INT");
                System.out.println("Column 'subject_id' added successfully.");

                // Add Foreign Key constraint
                stmt.executeUpdate(
                        "ALTER TABLE results ADD CONSTRAINT fk_results_subjects FOREIGN KEY (subject_id) REFERENCES subjects(id)");
                System.out.println("Foreign Key constraint added.");

            } catch (Exception e) {
                System.out.println("Error adding column (maybe it already exists?): " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
