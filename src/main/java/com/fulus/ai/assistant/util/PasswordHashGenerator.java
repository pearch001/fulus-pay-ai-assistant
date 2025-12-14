package com.fulus.ai.assistant.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Use this to create hashed passwords for manual admin user creation in database
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PasswordHashGenerator <password>");
            System.out.println("Example: java PasswordHashGenerator MySecurePassword123!");
            return;
        }

        String password = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);

        System.out.println("=================================================");
        System.out.println("Password Hash Generator");
        System.out.println("=================================================");
        System.out.println("Original Password: " + password);
        System.out.println("Hashed Password:   " + hashedPassword);
        System.out.println("=================================================");
        System.out.println("\nYou can use this hashed password in your SQL INSERT statement.");
        System.out.println("\nExample SQL:");
        System.out.println("INSERT INTO users (id, phone_number, name, email, password, role, active, balance, failed_login_attempts, account_number, created_at, updated_at)");
        System.out.println("VALUES (");
        System.out.println("  gen_random_uuid(),");
        System.out.println("  '08000000000',");
        System.out.println("  'Super Admin',");
        System.out.println("  'admin@fuluspay.com',");
        System.out.println("  '" + hashedPassword + "',");
        System.out.println("  'SUPER_ADMIN',");
        System.out.println("  true,");
        System.out.println("  0.00,");
        System.out.println("  0,");
        System.out.println("  'ADMIN-' || EXTRACT(EPOCH FROM NOW())::bigint,");
        System.out.println("  NOW(),");
        System.out.println("  NOW()");
        System.out.println(");");
    }
}

