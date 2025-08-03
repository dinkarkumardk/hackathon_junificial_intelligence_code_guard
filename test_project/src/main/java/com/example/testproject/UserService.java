package com.example.testproject;

import com.example.testproject.model.User;

public class UserService {
    public User createUser(String name, int age) {
        return new User(name, age);
    }
}

