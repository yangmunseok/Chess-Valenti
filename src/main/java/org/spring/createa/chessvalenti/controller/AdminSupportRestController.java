package org.spring.createa.chessvalenti.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inquiries")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminSupportRestController {

    private final InquiryService inquiryService;

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInquiry(@PathVariable int id) {
        log.info("Deleting inquiry {}", id);
        inquiryService.deleteInquiryById(id);
    }

    @PostMapping("/{id}/answer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answerInquiry(@PathVariable int id, @RequestBody AnswerRequest body) {
        log.info("Answering inquiry {}", id);
        inquiryService.answerInquiry(id, body.answer());
    }

    public record AnswerRequest(String answer) {}
}
