package com.service.users.service;

import java.util.UUID;

import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.service.users.io.CustomerRegistrationRequest;
import com.service.users.io.CustomerRegistrationResponse;
import com.service.users.io.SellerRegistrationRequest;
import com.service.users.io.SellerRegistrationResponse;
import com.service.users.model.CustomerEntity;
import com.service.users.model.SellerEntity;
import com.service.users.model.UserRole;
import com.service.users.repositories.CustomerRepository;
import com.service.users.repositories.SellerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {
    
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public CustomerRegistrationResponse registerCustomer(CustomerRegistrationRequest request){
        if(customerRepository.existsByEmail(request.getEmail())){
            throw new ResponseStatusException(HttpStatusCode.valueOf(409), "user with this email already exists, log in instead");
        }
        
        CustomerEntity customerEntity = CustomerEntity.builder()
            .userId(UUID.randomUUID().toString())
            .email(request.getEmail())
            .firstname(request.getFirstname())
            .lastname(request.getLastname())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.CUSTOMER)
            .build();
        
        customerEntity = customerRepository.save(customerEntity);
        return convertToCustomerResponse(customerEntity);
    }
    
    @Override
    public SellerRegistrationResponse registerSeller(SellerRegistrationRequest request){
        if(sellerRepository.existsByEmail(request.getEmail())){
            throw new ResponseStatusException(HttpStatusCode.valueOf(409), "user with this email already exists, log in instead");
        }
        
        SellerEntity sellerEntity = SellerEntity.builder()
            .userId(UUID.randomUUID().toString())
            .email(request.getEmail())
            .sellername(request.getSellername())
            .shopName(request.getShopName())
            .shippingAddress(request.getShippingAddress())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.SELLER)
            .build();
        
        sellerEntity = sellerRepository.save(sellerEntity);
        return convertToSellerResponse(sellerEntity);
    }

    private CustomerRegistrationResponse convertToCustomerResponse(CustomerEntity entity){
        return CustomerRegistrationResponse.builder()
            .userId(entity.getUserId())
            .firstname(entity.getFirstname())
            .lastname(entity.getLastname())
            .email(entity.getEmail())
            .build();
    }
    
    private SellerRegistrationResponse convertToSellerResponse(SellerEntity entity){
        return SellerRegistrationResponse.builder()
            .userId(entity.getUserId())
            .sellername(entity.getSellername())
            .shopName(entity.getShopName())
            .email(entity.getEmail())
            .build();
    }
}


