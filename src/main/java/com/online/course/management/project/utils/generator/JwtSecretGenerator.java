package com.online.course.management.project.utils.generator;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtSecretGenerator {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[32]; // 256 bits
        random.nextBytes(secret);
        String encodedSecret = Base64.getEncoder().encodeToString(secret);
        System.out.println("Generated JWT Secret Key: " + encodedSecret);
    }
}
