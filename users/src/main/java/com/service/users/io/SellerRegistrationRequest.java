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
public class SellerRegistrationRequest {
    
    private String sellername;
    private String shopName;
    private String email;
    private String password;
    private String shippingAddress;
}
