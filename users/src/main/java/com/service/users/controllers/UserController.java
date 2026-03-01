package com.service.users.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.service.users.io.RegisterRequest;
import com.service.users.io.RegisterResponse;
import com.service.users.repositories.UserRepository;
import com.service.users.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request){
        if (!userRepository.existsByEmail(request.getEmail())){
            return userService.register(request);
        }
        throw new RuntimeException("user already exists for this email");
    }
}
