package org.spring.createa.chessvalenti.domain;

public enum PaymentState {
  PAID,              // 결제 완료
  PAYMENT_FAILED,    // 결제 실패
  REFUNDED,          // 환불 완료
  REFUND_PENDING     // 환불 요청중
}
