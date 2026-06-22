package org.spring.createa.chessvalenti.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.request.PatchUserRequest;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin User REST API", description = "관리자 전용 사용자 역할 및 상태(밴) 관리 API")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminUserRestController {

    private final UserService userService;

    @Operation(summary = "사용자 정보 수정 (권한/차단)", description = "특정 사용자의 역할(Role) 및 차단(Ban) 여부를 수정합니다.")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUser(
            @Parameter(description = "수정할 사용자 ID") @PathVariable int id,
            @RequestBody PatchUserRequest body) {
        log.info("Updating user {} with body: {}", id, body);
        userService.patchUserRoleById(id, body.role(), body.ban());
    }
}
