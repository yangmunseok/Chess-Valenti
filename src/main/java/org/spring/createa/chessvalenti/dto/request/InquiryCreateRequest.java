package org.spring.createa.chessvalenti.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.spring.createa.chessvalenti.domain.InquiryCategory;

@Schema(description = "1:1 문의사항 생성 요청 DTO")
public record InquiryCreateRequest(
    @Schema(description = "문의 제목", example = "결제 관련 문의드립니다.") String title,
    @Schema(description = "문의 내용", example = "결제가 완료되었으나 등급 변경이 되지 않았습니다.") String content,
    @Schema(description = "문의 카테고리 (예: PAYMENT, SYSTEM, CHESS, ETC)") InquiryCategory category
) {

}
