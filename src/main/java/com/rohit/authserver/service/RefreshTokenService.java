package com.rohit.authserver.service;

import com.rohit.authserver.entity.RefreshToken;
import com.rohit.authserver.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyExpiration(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}