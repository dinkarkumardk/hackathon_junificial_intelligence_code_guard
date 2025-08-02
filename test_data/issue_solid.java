public class IssueSolid {
    // Violates Single Responsibility Principle
    public void processOrder(String orderId) {
        // Business logic
        System.out.println("Processing order: " + orderId);
        // Also handles logging (should be separate)
        logOrder(orderId);
    }

    private void logOrder(String orderId) {
        System.out.println("Order logged: " + orderId);
    }

    // Violates Open/Closed Principle
    public double calculateDiscount(String customerType, double amount) {
        if (customerType.equals("VIP")) {
            return amount * 0.2;
        } else if (customerType.equals("Regular")) {
            return amount * 0.1;
        } else {
            return 0;
        }
    }
}

