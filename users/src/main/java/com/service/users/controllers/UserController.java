package com.service.users.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.users.io.CustomerRegistrationRequest;
import com.service.users.io.CustomerRegistrationResponse;
import com.service.users.io.SellerRegistrationRequest;
import com.service.users.io.SellerRegistrationResponse;
import com.service.users.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @PostMapping("/register/customer")
    public CustomerRegistrationResponse registerCustomer(@RequestBody CustomerRegistrationRequest request){
        return userService.registerCustomer(request);
    }
    
    @PostMapping("/register/seller")
    public SellerRegistrationResponse registerSeller(@RequestBody SellerRegistrationRequest request){
        return userService.registerSeller(request);
    }
}
