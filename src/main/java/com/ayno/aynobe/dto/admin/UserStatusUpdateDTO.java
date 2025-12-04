package com.ayno.aynobe.dto.admin;

import com.ayno.aynobe.entity.enums.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserStatusUpdateDTO {
    private UserStatus status;
}
