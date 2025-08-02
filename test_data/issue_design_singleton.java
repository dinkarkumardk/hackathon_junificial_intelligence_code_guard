public class IssueDesignSingleton {
    // Poor Singleton implementation (thread-unsafe)
    private static IssueDesignSingleton instance;
    private IssueDesignSingleton() {}
    public static IssueDesignSingleton getInstance() {
        if (instance == null) {
            instance = new IssueDesignSingleton();
        }
        return instance;
    }
}

