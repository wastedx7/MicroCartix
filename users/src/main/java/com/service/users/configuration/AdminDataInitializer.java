package com.service.users.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.service.users.model.UserEntity;
import com.service.users.model.UserRole;
import com.service.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args){
        String adminEmail = "admin@system.com";
        if (userRepository.existsByEmail(adminEmail)){
            return;
        }
        UserEntity admin = UserEntity.builder()
            .email(adminEmail)
            .password(passwordEncoder.encode("Admin@123"))
            .role(UserRole.ADMIN)
            .build();

        userRepository.save(admin);

        System.out.println("default admin created : " + adminEmail);
    }
}
