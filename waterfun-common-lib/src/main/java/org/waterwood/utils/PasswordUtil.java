package org.waterwood.utils;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PasswordUtil {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final SecureRandom random = new SecureRandom();

    public static String generatePassword(int length){
        List<Character> passwordChars = new ArrayList<>();
        addRandomChar(passwordChars, UPPERCASE);
        addRandomChar(passwordChars, LOWERCASE);
        addRandomChar(passwordChars, DIGITS);
        addRandomChar(passwordChars, SPECIAL);
        String allChars = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
        for (int i = 4; i < length; i++) {
            addRandomChar(passwordChars, allChars);
        }
        Collections.shuffle(passwordChars, random);
        StringBuilder password = new StringBuilder();
        for (char c : passwordChars) {
            password.append(c);
        }
        return password.toString();
    }

    private static void addRandomChar(List<Character> list, String charSet) {
        int index = random.nextInt(charSet.length());
        list.add(charSet.charAt(index));
    }

    private static String BCryptEncoder(String raw){
        return BCrypt.hashpw(raw, BCrypt.gensalt());
    }

    private static boolean BCryptTryMatch(String raw, String encoded){
        return BCrypt.checkpw(raw, encoded);
    }
}
