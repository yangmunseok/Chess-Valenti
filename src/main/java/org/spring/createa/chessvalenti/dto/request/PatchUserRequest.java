package org.spring.createa.chessvalenti.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 수정 요청 DTO")
public record PatchUserRequest(
    @Schema(description = "변경할 사용자 역할 (예: ROLE_USER, ROLE_ADMIN)", example = "ROLE_USER") String role,
    @Schema(description = "사용자 차단 여부 (true: 차단, false: 정상)", example = "false") Boolean ban
) {

}
