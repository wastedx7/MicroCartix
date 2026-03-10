package com.service.users.service;

import com.service.users.io.RegisterRequest;
import com.service.users.io.RegisterResponse;

public interface UserService {
    
    //user registration 
    RegisterResponse register(RegisterRequest request);

}
