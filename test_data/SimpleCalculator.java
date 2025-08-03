/**
 * A simple calculator class for testing Code Guard analysis
 * This class has some basic code quality issues but is much smaller
 */
public class SimpleCalculator {
    
    // Public fields (code smell)
    public double result;
    public String lastOperation;
    
    // Constructor
    public SimpleCalculator() {
        this.result = 0.0;
        this.lastOperation = "none";
    }
    
    // Method with potential division by zero
    public double divide(double a, double b) {
        result = a / b; // No check for b == 0
        lastOperation = "division";
        return result;
    }
    
    // Method with magic numbers
    public double calculateTax(double amount) {
        return amount * 0.08; // Magic number for tax rate
    }
    
    // Method with poor error handling
    public double parseAndAdd(String num1, String num2) {
        try {
            double a = Double.parseDouble(num1);
            double b = Double.parseDouble(num2);
            result = a + b;
            return result;
        } catch (Exception e) {
            return -1; // Poor error handling
        }
    }
    
    // Unused method
    private void resetCalculator() {
        result = 0.0;
        lastOperation = "reset";
    }
    
    // Missing documentation
    public double multiply(double x, double y) {
        result = x * y;
        return result;
    }
}
