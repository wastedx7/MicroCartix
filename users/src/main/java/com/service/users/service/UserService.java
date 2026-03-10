package com.service.users.service;

import com.service.users.io.CustomerRegistrationRequest;
import com.service.users.io.CustomerRegistrationResponse;
import com.service.users.io.SellerRegistrationRequest;
import com.service.users.io.SellerRegistrationResponse;

public interface UserService {
    
    //customer registration 
    CustomerRegistrationResponse registerCustomer(CustomerRegistrationRequest request);
    
    //seller registration
    SellerRegistrationResponse registerSeller(SellerRegistrationRequest request);
    
}

