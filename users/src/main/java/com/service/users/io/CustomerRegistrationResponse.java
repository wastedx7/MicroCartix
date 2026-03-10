package com.service.users.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CustomerRegistrationResponse {
    
    private String userId;
    private String firstname;
    private String lastname;
    private String email;   
    
}
