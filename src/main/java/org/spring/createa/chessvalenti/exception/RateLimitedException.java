package org.spring.createa.chessvalenti.exception;

/**
 * 외부 API가 429(Too Many Requests)로 요청을 거절했고, 기다렸다 다시 시도해도 풀리지 않았을 때 던진다.
 */
public class RateLimitedException extends RuntimeException {

  public RateLimitedException(String message) {
    super(message);
  }
}
