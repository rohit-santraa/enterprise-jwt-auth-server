package com.rohit.authserver.service;

import com.rohit.authserver.dto.request.LoginRequest;
import com.rohit.authserver.dto.request.RefreshTokenRequest;
import com.rohit.authserver.dto.request.RegistrationRequest;
import com.rohit.authserver.dto.response.ApiResponse;
import com.rohit.authserver.dto.response.JwtResponse;

public interface AuthService {
    ApiResponse<Void> registerUser(RegistrationRequest request);
    JwtResponse authenticateUser(LoginRequest request);
    JwtResponse refreshToken(RefreshTokenRequest request);
    ApiResponse<Void> logout(RefreshTokenRequest request);
}
