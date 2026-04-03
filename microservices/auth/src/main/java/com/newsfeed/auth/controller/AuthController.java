package com.newsfeed.auth.controller;

import com.newsfeed.auth.dto.*;
import com.newsfeed.auth.exception.ErrorResponse;
import com.newsfeed.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, logout and token validation")
public class AuthController {

    private final AuthService authService;

    // ─── Register ────────────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email / username already in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    @Operation(summary = "Login with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─── Refresh token ───────────────────────────────────────────────────────────

    @Operation(summary = "Refresh access token using a valid refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid / expired / revoked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // ─── Logout ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Logout – revoke all refresh tokens for the current user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearerToken) {
        String token = extractBearer(bearerToken);
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    // ─── Validate token (internal / gateway use) ─────────────────────────────────

    @Operation(summary = "Validate an access token and return user claims",
            description = "Intended for internal use by other micro-services / API gateway.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token info returned (valid flag may be false)",
                    content = @Content(schema = @Schema(implementation = ValidateTokenResponse.class)))
    })
    @GetMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(
            @RequestHeader("Authorization") String bearerToken) {
        String token = extractBearer(bearerToken);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private String extractBearer(String header) {
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}

