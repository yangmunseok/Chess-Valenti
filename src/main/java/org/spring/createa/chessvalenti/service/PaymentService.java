package org.spring.createa.chessvalenti.service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.PaymentRepository;
import org.spring.createa.chessvalenti.domain.Payment;
import org.spring.createa.chessvalenti.domain.PaymentState;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.request.PaymentConfirmRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

  @Value("${toss.payment.secret-key}")
  private String widgetSecretKey;

  private final UserService userService;
  private final PaymentRepository paymentRepository;
  private final WebClient webClient;

  @Transactional
  public Map<String, Object> confirmPayment(User user, PaymentConfirmRequest body) {
    String authorizations = "Basic " + Base64.getEncoder()
        .encodeToString((widgetSecretKey + ":").getBytes());

    Map<String, Object> response = webClient.post()
        .uri("https://api.tosspayments.com/v1/payments/confirm")
        .header(HttpHeaders.AUTHORIZATION, authorizations)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class)
        .cast(Map.class)
        .block(); // 비동기 흐름을 맞추기 위해 현재는 block 사용 (추후 전체 비동기 전환 가능)

    if (response != null) {
      int amount = Integer.parseInt(body.amount());
      savePayment(user, amount, "toss", "donation");
      userService.addDonation(user, amount);
    }

    return response;
  }

  public void savePayment(User user, int amount, String method, String product) {
    Payment payment = new Payment();
    payment.setUser(user);
    payment.setAmount(amount);
    payment.setMethod(method);
    payment.setState(PaymentState.PAID);
    payment.setProduct(product);
    paymentRepository.save(payment);
  }

  public void requestRefund(User user, int id) {
    Payment payment = paymentRepository.findById(id).orElse(null);

    if (payment != null && payment.getUser().equals(user)) {
      if (payment.getState() == PaymentState.PAID) {
        payment.setState(PaymentState.REFUND_PENDING);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
      }
    }
  }

  public void acceptRefund(int id) {
    Payment payment = paymentRepository.findById(id).orElse(null);
    if (payment != null && payment.getState() == PaymentState.REFUND_PENDING) {
      payment.setState(PaymentState.REFUNDED);
      payment.setUpdatedAt(LocalDateTime.now());
      paymentRepository.save(payment);
    }
  }

  public void rejectRefund(int id) {
    Payment payment = paymentRepository.findById(id).orElse(null);
    if (payment != null && payment.getState() == PaymentState.REFUND_PENDING) {
      payment.setState(PaymentState.PAID);
      payment.setUpdatedAt(LocalDateTime.now());
      paymentRepository.save(payment);
    }
  }

  public Page<Payment> findAll(Pageable pageable) {
    Sort sort = Sort.by(Sort.Order.desc("createdAt"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);
    return paymentRepository.findAll(pageRequest);
  }
}
