package com.service.users.controllers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.service.users.io.AuthRequest;
import com.service.users.io.AuthResponse;
import com.service.users.service.AppUserDetailsService;
import com.service.users.utility.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AppUserDetailsService appUserDetailsService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try{
            // if credentials match, use authenticate to authenticate then do futher stuff
            authenticate(authRequest.getEmail(), authRequest.getPassword()); 
            // go to authUserDetailsService and load user by email you get from authRequest, store in userDetails somehow
            final UserDetails userDetails = appUserDetailsService.loadUserByUsername(authRequest.getEmail());
            final String jwtToken = jwtUtil.generateToken(userDetails);
            ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite("Strict")
                .build();
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new AuthResponse(authRequest.getEmail(), jwtToken));
        } 
        catch(BadCredentialsException ex) { // if email, password wrong, use this
            Map<String, Object> error = new HashMap<>();                        
            error.put("error", true);
            error.put("message", "email or password is incorrect");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } 
        catch(DisabledException ex) { // if account disabled, use this
            Map<String, Object> error = new HashMap<>();                        
            error.put("error", true);
            error.put("message", "account is disabled");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } 
        catch(Exception ex) { // for unauthorized attemps
            Map<String, Object> error = new HashMap<>();                        
            error.put("error", true);
            error.put("message", "authorization failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    private void authenticate(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }  

}
