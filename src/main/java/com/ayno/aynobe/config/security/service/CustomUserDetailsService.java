package com.ayno.aynobe.config.security.service;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.UserStatus;
import com.ayno.aynobe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저가 존제하지 않습니다 : " + username));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new LockedException("관리자에 의해 차단된 계정입니다.");
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new DisabledException("탈퇴한 계정입니다.");
        }

        return new CustomUserDetails(user);
    }
}
