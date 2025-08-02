public class IssueSolidLiskov {
    // Violates Liskov Substitution Principle
    class Bird {
        public void fly() {
            System.out.println("Flying");
        }
    }
    class Ostrich extends Bird {
        @Override
        public void fly() {
            throw new UnsupportedOperationException("Ostrich can't fly");
        }
    }
}

