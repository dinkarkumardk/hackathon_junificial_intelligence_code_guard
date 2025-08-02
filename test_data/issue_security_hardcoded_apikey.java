public class IssueSecurityHardcodedApiKey {
    // Hardcoded API key (security issue)
    private static final String API_KEY = "sk-test-1234567890";

    public void connectToService() {
        System.out.println("Connecting with API key: " + API_KEY);
    }
}

