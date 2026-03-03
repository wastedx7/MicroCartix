package com.service.users.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.service.users.model.AdminEntity;
import com.service.users.model.UserRole;
import com.service.users.repositories.AdminRepository;

import lombok.RequiredArgsConstructor;

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
            .email(adminEmail)
            .password(passwordEncoder.encode("Admin@123"))
            .role(UserRole.ADMIN)
            .build();

        adminRepository.save(admin);

        System.out.println("default admin created : " + adminEmail);
    }
}
