public class MetricsExample {
    private String name;
    private int age;
    
    // Constructor with parameters
    public MetricsExample(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // Getter method for name
    public String getName() {
        return name;
    }
    
    // Setter method for name  
    public void setName(String name) {
        this.name = name;
    }
    
    // Getter method for age
    public int getAge() {
        return age;
    }
    
    // Setter method for age
    public void setAge(int age) {
        this.age = age;
    }
    
    // Complex method with higher cyclomatic complexity
    public String processAge(boolean includeDetails) {
        if (age < 0) {
            return "Invalid age";
        } else if (age < 18) {
            if (includeDetails) {
                return "Minor: " + name + " is " + age + " years old";
            } else {
                return "Minor";
            }
        } else if (age < 65) {
            if (includeDetails) {
                return "Adult: " + name + " is " + age + " years old";
            } else {
                return "Adult";
            }
        } else {
            if (includeDetails) {
                return "Senior: " + name + " is " + age + " years old";
            } else {
                return "Senior";
            }
        }
    }
}
