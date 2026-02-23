package com.service.users.io;

import lombok.Builder;

@Builder
public class RegisterResponse {
    
    private String userId;
    private String firstname;
    private String lastname;
    private String email;   
    
}
