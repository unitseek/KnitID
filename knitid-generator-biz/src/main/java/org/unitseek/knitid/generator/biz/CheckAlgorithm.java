package org.unitseek.knitid.generator.biz;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;

/**
 * Supported check-digit algorithms.
 */
public enum CheckAlgorithm {

    /** ISO 7064 MOD 37,36 (alphanumeric, default). */
    MOD37_36("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ") {
        @Override
        public char checkDigit(String content) {
            String value = content.toUpperCase(java.util.Locale.ROOT);
            int modulus = 36;
            int check = modulus / 2;
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                int digit = ALPHABET.indexOf(ch);
                if (digit < 0) {
                    if (!Character.isLetterOrDigit(ch)) {
                        continue;
                    }
                    throw invalid("MOD37_36 only supports 0-9A-Z content: " + content);
                }
                int current = check == 0 ? modulus : check;
                check = (((current * 2) % (modulus + 1)) + digit) % modulus;
            }
            int current = check == 0 ? modulus : check;
            int c = Math.floorMod(1 - ((current * 2) % (modulus + 1)), modulus);
            return ALPHABET.charAt(c);
        }
    },

    /** ISO 7064 MOD 11,2 (numeric; may produce X). */
    MOD11_2("0123456789") {
        @Override
        public char checkDigit(String content) {
            int check = 0;
            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (!Character.isLetterOrDigit(ch)) {
                    continue;
                }
                int digit;
                if (ch == 'X' || ch == 'x') {
                    digit = 10;
                }
                else if (ch >= '0' && ch <= '9') {
                    digit = ch - '0';
                }
                else {
                    throw invalid("MOD11_2 only supports numeric content (and X): " + content);
                }
                check = (2 * check + digit) % 11;
            }
            int c = Math.floorMod(1 - (2 * check), 11);
            return c == 10 ? 'X' : (char) ('0' + c);
        }
    },

    /** Luhn (numeric). */
    LUHN("0123456789") {
        @Override
        public char checkDigit(String content) {
            int sum = 0;
            boolean doubleDigit = true;
            for (int i = content.length() - 1; i >= 0; i--) {
                char ch = content.charAt(i);
                if (!Character.isLetterOrDigit(ch)) {
                    continue;
                }
                if (ch < '0' || ch > '9') {
                    throw invalid("LUHN only supports numeric content: " + content);
                }
                int digit = ch - '0';
                if (doubleDigit) {
                    digit *= 2;
                    if (digit > 9) {
                        digit -= 9;
                    }
                }
                sum += digit;
                doubleDigit = !doubleDigit;
            }
            int c = (10 - (sum % 10)) % 10;
            return (char) ('0' + c);
        }
    },

    /** No check digit. */
    NONE("") {
        @Override
        public char checkDigit(String content) {
            throw invalid("Check algorithm NONE cannot be used with a {check} placeholder");
        }
    };

    protected static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final String alphabet;

    CheckAlgorithm(String alphabet) {
        this.alphabet = alphabet;
    }

    public String alphabet() {
        return alphabet;
    }

    public abstract char checkDigit(String content);

    private static IdGenerationException invalid(String message) {
        return new IdGenerationException(IdErrorCodes.GENERATION_FAILED, message);
    }
}
