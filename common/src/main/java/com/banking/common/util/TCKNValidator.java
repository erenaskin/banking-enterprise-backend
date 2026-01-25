package com.banking.common.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TCKNValidator implements ConstraintValidator<TCKN, String> {

    @Override
    public boolean isValid(String tckn, ConstraintValidatorContext context) {
        if (tckn == null || tckn.length() != 11 || !tckn.matches("^\\d+$")) {
            return false;
        }

        int[] digits = new int[11];
        for (int i = 0; i < 11; i++) {
            digits[i] = Integer.parseInt(tckn.substring(i, i + 1));
        }

        if (digits[0] == 0) {
            return false;
        }

        int sum1 = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int sum2 = digits[1] + digits[3] + digits[5] + digits[7];

        int tenthDigit = ((sum1 * 7) - sum2) % 10;
        if (tenthDigit < 0) {
            tenthDigit += 10;
        }

        if (digits[9] != tenthDigit) {
            return false;
        }

        int eleventhDigit = (sum1 + sum2 + tenthDigit) % 10;
        if (eleventhDigit < 0) {
            eleventhDigit += 10;
        }

        return digits[10] == eleventhDigit;
    }
}