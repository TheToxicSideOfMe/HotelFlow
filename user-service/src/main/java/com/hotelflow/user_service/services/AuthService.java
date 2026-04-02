package com.hotelflow.user_service.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hotelflow.user_service.dtos.LoginRequest;
import com.hotelflow.user_service.dtos.LoginResponse;
import com.hotelflow.user_service.dtos.RegisterRequest;
import com.hotelflow.user_service.dtos.RegisterResponse;
import com.hotelflow.user_service.exception.InvalidCredentialsException;
import com.hotelflow.user_service.exception.UserAlreadyExistsException;
import com.hotelflow.user_service.models.User;
import com.hotelflow.user_service.models.User.UserRole;
import com.hotelflow.user_service.repositories.RefreshTokenRepository;
import com.hotelflow.user_service.repositories.UserRepository;
import com.hotelflow.user_service.security.JwtUtil;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired 
    private AuthenticationManager authenticationManager;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RegisterResponse registerUser(RegisterRequest request){
        //check if user exists by email or username
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        User newUser=new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); 
        newUser.setFirstName(request.getName());
        newUser.setLastName(request.getLastName());
        newUser.setPhone(request.getPhone());

        newUser.setRole(UserRole.CUSTOMER); //set customer by default
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setEnabled(true);

        User savedUser=userRepository.save(newUser);
        return RegisterResponse.from(savedUser);
    }

    
    
    @Transactional
    public LoginResponse loginUser(LoginRequest request){
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        //load user
        User user = userRepository.findByUsername(request.getUsername())
                        .orElseThrow(()-> new InvalidCredentialsException("User Not Found"));
        String accessToken = jwtUtil.generateAccessToken(request.getUsername(),user.getRole().name(),user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(request.getUsername());

        refreshTokenService.saveRefreshToken(user, refreshToken);

        return new LoginResponse(
            accessToken,
            refreshToken,
            "Bearer",
            15 * 60 * 1000L, 
            RegisterResponse.from(user)
        );

    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }


    @Transactional
    public LoginResponse refreshTokens(String token){

        System.out.println(token);

        String username = jwtUtil.extractUsername(token);

        System.out.println(username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(()-> new InvalidCredentialsException("User Not Found"));

        if (jwtUtil.validateRefreshToken(token, username)==false) {
            throw new InvalidCredentialsException("Invalid Refresh Token");
        }
        

        //generate new access and refresh tokens
        String accessToken = jwtUtil.generateAccessToken(username, user.getRole().name(),user.getId());;
        String refreshToken=jwtUtil.generateRefreshToken(username);
        refreshTokenService.saveRefreshToken(user, refreshToken);

        return new LoginResponse(
            accessToken,
            refreshToken,
            "Bearer",
            15 * 60 * 1000L, 
            RegisterResponse.from(user)
        );

    }
}
