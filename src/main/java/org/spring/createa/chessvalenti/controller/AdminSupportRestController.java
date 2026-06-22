package org.spring.createa.chessvalenti.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Support REST API", description = "관리자 전용 1:1 문의 관리 및 답변 API")
@RestController
@RequestMapping("/api/admin/inquiries")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminSupportRestController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의사항 삭제", description = "특정 1:1 문의글을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInquiry(@Parameter(description = "삭제할 문의사항 ID") @PathVariable int id) {
        log.info("Deleting inquiry {}", id);
        inquiryService.deleteInquiryById(id);
    }

    @Operation(summary = "문의사항 답변 등록", description = "특정 1:1 문의글에 답변을 등록하거나 수정합니다.")
    @PostMapping("/{id}/answer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answerInquiry(
            @Parameter(description = "답변할 문의사항 ID") @PathVariable int id,
            @RequestBody AnswerRequest body) {
        log.info("Answering inquiry {}", id);
        inquiryService.answerInquiry(id, body.answer());
    }

    public record AnswerRequest(
            @Schema(description = "답변 내용") String answer) {}
}
