package com.service.users.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.service.users.repositories.AdminRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminRepository adminRepository;

    public void getAllProducts(){
        /* this is a future function to return all available products */
    }
}
