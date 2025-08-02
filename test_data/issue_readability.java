public class IssueReadability {
    // Poor variable naming and lack of comments
    public void a(int b) {
        int c = 0;
        for (int d = 0; d < b; d++) {
            c += d;
        }
        System.out.println(c);
    }

    // Deeply nested code
    public void check(int x) {
        if (x > 0) {
            if (x < 100) {
                if (x % 2 == 0) {
                    System.out.println("Even and in range");
                }
            }
        }
    }
}

