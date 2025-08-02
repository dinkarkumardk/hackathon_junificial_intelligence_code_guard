package com.hackathon.codeguard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CodeGuardApplication
 */
class CodeGuardApplicationTest {

    @Test
    void testApplicationStartup() {
        // Test that the application can be instantiated
        assertDoesNotThrow(() -> {
            CodeGuardApplication app = new CodeGuardApplication();
            assertNotNull(app);
        });
    }
}
