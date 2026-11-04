package com.workflow.approval_workflow.service;

import com.workflow.approval_workflow.domain.AppUser;
import com.workflow.approval_workflow.dto.LoginRequest;
import com.workflow.approval_workflow.dto.LoginResponse;
import com.workflow.approval_workflow.exception.ResourceNotFoundException;
import com.workflow.approval_workflow.repository.UserRepository;
import com.workflow.approval_workflow.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /** Verifies the credentials (BadCredentialsException on failure) and issues a signed JWT. */
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User '" + request.username() + "' does not exist"));
        String role = user.getRole().getName();
        String token = jwtService.generateToken(user.getUsername(), role);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(), user.getUsername(), role);
    }
}
