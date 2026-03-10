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
public class SellerRegistrationResponse {
    
    private String userId;
    private String sellername;
    private String shopName;
    private String email;
    
}
