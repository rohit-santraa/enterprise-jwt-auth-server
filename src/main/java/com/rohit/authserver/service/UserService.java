package com.rohit.authserver.service;

import com.rohit.authserver.dto.request.UpdateProfileRequest;
import com.rohit.authserver.dto.response.ProfileResponse;
import com.rohit.authserver.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getUserByUsername(String username);
    List<UserResponse> getAllUsers();
    ProfileResponse getProfile(String email);
    ProfileResponse updateProfile(String email, UpdateProfileRequest request);
}
