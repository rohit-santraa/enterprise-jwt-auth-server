package com.rohit.authserver.service.impl;

import com.rohit.authserver.dto.request.LoginRequest;
import com.rohit.authserver.dto.request.RefreshTokenRequest;
import com.rohit.authserver.dto.request.RegistrationRequest;
import com.rohit.authserver.dto.response.ApiResponse;
import com.rohit.authserver.dto.response.JwtResponse;
import com.rohit.authserver.entity.RefreshToken;
import com.rohit.authserver.entity.Role;
import com.rohit.authserver.entity.User;
import com.rohit.authserver.exception.TokenExpiredException;
import com.rohit.authserver.exception.UserAlreadyExistsException;
import com.rohit.authserver.repository.RoleRepository;
import com.rohit.authserver.repository.UserRepository;
import com.rohit.authserver.security.CustomUserDetails;
import com.rohit.authserver.service.AuthService;
import com.rohit.authserver.service.JwtService;
import com.rohit.authserver.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public ApiResponse<Void> registerUser(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(com.rohit.authserver.entity.ERole.ROLE_USER)
                .ifPresent(roles::add);
        user.setRoles(roles);

        userRepository.save(user);
        return new ApiResponse<>(201, "User registered successfully", null);
    }

    @Override
    public JwtResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new JwtResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                user.getId().longValue(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(r -> r.getName().name()).toList()
        );
    }

    @Override
    public JwtResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenExpiredException("Refresh token not found"));

        refreshTokenService.verifyExpiration(token);

        User user = token.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return new JwtResponse(
                newAccessToken,
                token.getToken(),
                "Bearer",
                user.getId().longValue(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(r -> r.getName().name()).toList()
        );
    }

    @Override
    @Transactional
    public ApiResponse<Void> logout(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenExpiredException("Refresh token not found"));

        refreshTokenService.deleteByUser(token.getUser());
        return new ApiResponse<>(200, "Logged out successfully", null);
    }
}
