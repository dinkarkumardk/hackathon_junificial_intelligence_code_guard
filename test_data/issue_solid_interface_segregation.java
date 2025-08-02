public class IssueSolidInterfaceSegregation {
    // Violates Interface Segregation Principle
    interface Worker {
        void work();
        void eat();
    }
    class Robot implements Worker {
        public void work() {
            System.out.println("Robot working");
        }
        public void eat() {
            // Not applicable for robots
            throw new UnsupportedOperationException("Robot can't eat");
        }
    }
}

