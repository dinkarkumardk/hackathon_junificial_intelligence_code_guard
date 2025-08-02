package com.example.testproject;

import com.example.testproject.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    @Test
    void testCreateUser() {
        UserService service = new UserService();
        User user = service.createUser("Bob", 25);
        assertEquals("Bob", user.getName());
        assertEquals(25, user.getAge());
    }
}

