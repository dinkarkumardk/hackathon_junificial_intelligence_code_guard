public class IssueSecurity {
    // Hardcoded credentials (security issue)
    private String dbUser = "admin";
    private String dbPassword = "password123";

    public void connect() {
        // Simulate DB connection using hardcoded credentials
        System.out.println("Connecting to DB with user: " + dbUser);
    }

    // SQL Injection vulnerability
    public void getUser(String username) {
        String query = "SELECT * FROM users WHERE username = '" + username + "'";
        System.out.println("Executing query: " + query);
    }
}

