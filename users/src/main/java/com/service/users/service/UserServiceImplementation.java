package com.service.users.service;

import java.util.UUID;

import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.service.users.io.RegisterRequest;
import com.service.users.io.RegisterResponse;
import com.service.users.model.UserEntity;
import com.service.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public RegisterResponse register(RegisterRequest request){
        UserEntity userEntity = convertToUserEntity(request);
        if(!userRepository.existsByEmail(request.getEmail())){
            userEntity = userRepository.save(userEntity);
            return convertToResponseEntity(userEntity);
        }
        throw new ResponseStatusException(HttpStatusCode.valueOf(409), "user with this email already exists");
    }

    private UserEntity convertToUserEntity(RegisterRequest request){
        return UserEntity.builder()
            .userId(UUID.randomUUID().toString())
            .email(request.getEmail())
            .firstname(request.getFirstname())
            .lastname(request.getLastname())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();
    }

    private RegisterResponse convertToResponseEntity(UserEntity entity){
        return RegisterResponse.builder()
            .userId(entity.getUserId())
            .firstname(entity.getFirstname())
            .lastname(entity.getLastname())
            .email(entity.getEmail())
            .build();
    }
}

