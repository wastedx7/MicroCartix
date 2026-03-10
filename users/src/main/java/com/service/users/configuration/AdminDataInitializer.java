package com.service.users.configuration;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.service.users.model.AdminEntity;
import com.service.users.model.UserRole;
import com.service.users.repositories.AdminRepository;

import lombok.RequiredArgsConstructor;


// ive made a simple one admin for simplicity 
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args){
        String adminEmail = "admin@system.com";
        if (adminRepository.existsByEmail(adminEmail)){
            return;
        }
        AdminEntity admin = AdminEntity.builder()
            .userId(UUID.randomUUID().toString())
            .email(adminEmail)
            .password(passwordEncoder.encode("Admin@123"))
            .createdAt(LocalDateTime.now())
            .role(UserRole.ADMIN)
            .build();

        adminRepository.save(admin);

        System.out.println("default admin created : " + adminEmail);
    }
}
