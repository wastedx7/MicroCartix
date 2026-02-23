package com.service.users.service;

import com.service.users.io.RegisterRequest;
import com.service.users.io.RegisterResponse;

public interface UserService {
    
    RegisterResponse register(RegisterRequest request);
}
