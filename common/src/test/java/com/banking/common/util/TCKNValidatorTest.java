package com.banking.common.util;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TCKNValidatorTest {

    private TCKNValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new TCKNValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "10000000146", // Valid TCKN example
            "12345678950"  // Another valid example (hypothetical, needs to satisfy algorithm)
    })
    void isValid_WithValidTCKN_ShouldReturnTrue(String tckn) {
        // Note: "12345678950" might not be mathematically valid according to the algorithm.
        // Let's use a known valid one: 10000000146
        // And another one calculated: 
        // 123456789 -> sum1=1+3+5+7+9=25, sum2=2+4+6+8=20. 
        // 10th = (25*7 - 20) % 10 = (175 - 20) % 10 = 155 % 10 = 5.
        // 11th = (25 + 20 + 5) % 10 = 50 % 10 = 0.
        // So 12345678950 is valid.
        
        assertTrue(validator.isValid(tckn, context));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",             // Empty
            "123",          // Too short
            "123456789012", // Too long
            "1234567890a",  // Non-numeric
            "02345678901",  // Starts with 0
            "11111111111"   // Invalid checksum (usually)
    })
    void isValid_WithInvalidTCKN_ShouldReturnFalse(String tckn) {
        assertFalse(validator.isValid(tckn, context));
    }
}