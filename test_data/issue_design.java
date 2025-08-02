public class IssueDesign {
    // God class: too many responsibilities
    public void processPayment() {
        System.out.println("Processing payment...");
    }

    public void generateInvoice() {
        System.out.println("Generating invoice...");
    }

    public void sendEmail() {
        System.out.println("Sending email...");
    }

    // Tight coupling
    private IssueSolid solidDependency = new IssueSolid();
    public void useSolid() {
        solidDependency.processOrder("123");
    }
}

