package com.ayno.aynobe.service.admin;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.admin.AdminUserResponseDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.UserStatus;
import com.ayno.aynobe.repository.UserRepository; // JpaRepository + Custom
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {
    private final UserRepository userRepository;

    public PageResponseDTO<AdminUserResponseDTO> getUsers(
            UserStatus status,
            String keyword,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        // 이부분 잘 모르겠다 -> 날짜를 일시로 변환하는 거라는데 왜 변환해야하고 from과 to에 붙은 저 함수들은 뭐지? 처읍본다
        LocalDateTime startAt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime endAt = (to != null) ? to.atTime(LocalTime.MAX) : null;

        Page<User> userPage = userRepository.searchUsers(status, keyword, startAt, endAt, pageable);

        List<AdminUserResponseDTO> content = userPage.getContent().stream()
                .map(AdminUserResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.<AdminUserResponseDTO>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .hasNext(userPage.hasNext())
                .build();
    }

    @Transactional
    public void changeUserStatus(Long userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("유저아이디 " + userId + " 는 존재하지 않습니다"));

        // 로직: 이미 탈퇴한 회원은 상태 변경 불가 등 방어 로직 추가 가능
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw CustomException.badRequest("탈퇴한 회원의 상태는 변경할 수 없습니다.");
        }
        user.changeStatus(newStatus);
    }
}
