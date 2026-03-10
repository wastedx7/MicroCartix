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
public class CustomerRegistrationRequest {
    
    private String firstname;
    private String lastname;
    private String email;
    private String password;
}
