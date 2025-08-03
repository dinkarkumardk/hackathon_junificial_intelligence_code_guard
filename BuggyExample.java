public class BuggyExample {
    
    public String processData(String[] data) {
        // Potential null pointer exception - no null check
        String result = data[0].toUpperCase();
        
        // Potential array index out of bounds - no length check
        for (int i = 0; i <= data.length; i++) {
            result += data[i];
        }
        
        // Resource leak - no try-with-resources
        FileReader reader = new FileReader("test.txt");
        BufferedReader bufferedReader = new BufferedReader(reader);
        
        return result;
    }
    
    public void infiniteLoop() {
        int i = 0;
        // Potential infinite loop - condition never changes
        while (i < 10) {
            System.out.println("This might run forever");
        }
    }
    
    public boolean compareStrings(String str1, String str2) {
        // Bug: using == instead of equals() for string comparison
        return str1 == str2;
    }
}
