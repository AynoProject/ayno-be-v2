package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.config.security.service.JwtService;
import com.ayno.aynobe.dto.auth.*;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.AgeBand;
import com.ayno.aynobe.entity.enums.GenderType;
import com.ayno.aynobe.entity.enums.UsageDepthType;
import com.ayno.aynobe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(@Qualifier("userAuthManager") AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginTokensDTO login(LoginRequestDTO request) {

        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        String access  = jwtService.generateAccessToken(principal);
        String refresh = jwtService.generateRefreshToken(principal);

        return LoginTokensDTO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();
    }

    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO request) {
        User user = User.builder()
                .username(request.getUsername())
                .nickname(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .gender(GenderType.NONE)
                .aiUsageDepth(UsageDepthType.NONE)
                .ageBand(AgeBand.NONE)
                .build();

        try {
            userRepository.saveAndFlush(user);
            return new SignUpResponseDTO("회원가입이 완료되었습니다.");
        } catch (DataIntegrityViolationException e) {
            throw CustomException.duplicate("이미 사용 중인 아이디입니다.");
        }
    }

    @Transactional(readOnly = true)
    public DuplicationResponseDTO checkUsername(String username) {
        boolean isDuplicated = userRepository.existsByUsername(username);

        if (isDuplicated)
            return new DuplicationResponseDTO(false, "이미 사용 중인 아이디입니다.");

        return new DuplicationResponseDTO(true, "사용 가능한 아이디입니다.");
    }
}
