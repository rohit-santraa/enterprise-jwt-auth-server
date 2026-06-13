package com.rohit.authserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {

        @NotBlank(message = "JWT secret must not be blank")
        private String secret;

        @Positive(message = "JWT expiration must be a positive value")
        private long expiration;

        @Positive(message = "Refresh token expiration must be a positive value")
        private long refreshExpiration;

    }

}
