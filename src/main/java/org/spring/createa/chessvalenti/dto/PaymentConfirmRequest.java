package org.spring.createa.chessvalenti.dto;

public record PaymentConfirmRequest(String paymentKey, String amount, String orderId) {

  @Override
  public String toString() {
    return "{" +
        "\"paymentKey\":\"" + paymentKey + '\"' +
        ", \"amount\":\"" + amount + '\"' +
        ", \"orderId\":\"" + orderId + '\"' +
        '}';
  }
}
