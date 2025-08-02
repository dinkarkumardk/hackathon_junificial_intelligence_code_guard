package com.example.testproject;

public class MainApp {
    public static void main(String[] args) {
        System.out.println("Welcome to Test Project!");
        UserService userService = new UserService();
        User user = userService.createUser("Alice", 30);
        System.out.println("Created user: " + user);
    }
}

