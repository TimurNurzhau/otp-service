package com.example;

import com.example.model.User;
import com.example.service.UserService;

public class CreateAdmin {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Creating Admin User");
        System.out.println("========================================");

        try {
            UserService userService = new UserService();

            // Проверяем, существует ли уже админ
            var allUsers = userService.findAll();
            boolean adminExists = allUsers.stream()
                    .anyMatch(u -> u.getRole() == User.Role.ADMIN);

            if (adminExists) {
                System.out.println("Admin user already exists!");
                System.out.println("\nExisting admins:");
                allUsers.stream()
                        .filter(u -> u.getRole() == User.Role.ADMIN)
                        .forEach(u -> System.out.println("  - Username: " + u.getUsername()));
            } else {
                // Создаем админа
                User admin = userService.register("admin", "admin123", User.Role.ADMIN);
                System.out.println("✅ Admin created successfully!");
                System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("  Username: admin");
                System.out.println("  Password: admin123");
                System.out.println("  Role: ADMIN");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }

        } catch (IllegalStateException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error creating admin: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n========================================");
        System.out.println("You can now login with:");
        System.out.println("  POST http://localhost:8081/api/login");
        System.out.println("  Body: {\"username\":\"admin\",\"password\":\"admin123\"}");
        System.out.println("========================================");
    }
}