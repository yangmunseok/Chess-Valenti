package org.spring.createa.chessvalenti.controller;

import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<String> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    log.error("Data Integrity Violation: ", e);
    String message = e.getMostSpecificCause().getMessage();
    if (message != null) {
      String lowerMsg = message.toLowerCase();
      if (lowerMsg.contains("uk_post_title") || lowerMsg.contains("title")) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 제목입니다.");
      }
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("데이터 처리 중 오류가 발생했습니다.");
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleAllException(Exception e) {
    // 콘솔에 에러의 구체적인 원인을 찍습니다.
    log.error("400 Error Trace: ", e);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  }
}