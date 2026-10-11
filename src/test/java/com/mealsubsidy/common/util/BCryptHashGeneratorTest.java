package com.mealsubsidy.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptHashGeneratorTest {

    @Test
    void encodeWithJavaBCryptThenMatch() {
        String encoded = BCryptHashGenerator.encode("123456");
        assertTrue(encoded.startsWith("$2a$"));
        assertTrue(BCryptHashGenerator.matches("123456", encoded));
        assertFalse(BCryptHashGenerator.matches("654321", encoded));
    }
}
