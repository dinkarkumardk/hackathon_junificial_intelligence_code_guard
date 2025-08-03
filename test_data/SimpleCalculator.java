public class SimpleCalculator {
    
    // Basic arithmetic operations
    public double add(double a, double b) {
        return a + b;
    }
    
    public double subtract(double a, double b) {
        return a - b;
    }
    
    public double multiply(double a, double b) {
        return a * b;
    }
    
    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return a / b;
    }
    
    // Advanced operations
    public double power(double base, double exponent) {
        return Math.pow(base, exponent);
    }
    
    public double squareRoot(double number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot calculate square root of negative number");
        }
        return Math.sqrt(number);
    }
    
    // Utility methods
    public double percentage(double value, double percentage) {
        return (value * percentage) / 100;
    }
    
    public boolean isEven(int number) {
        return number % 2 == 0;
    }
    
    public boolean isOdd(int number) {
        return number % 2 != 0;
    }
    
    // Method with potential issues for testing
    public double calculateCompoundInterest(double principal, double rate, int time) {
        // Simple compound interest calculation
        // A = P(1 + r/100)^t
        return principal * Math.pow(1 + rate/100, time);
    }
}
