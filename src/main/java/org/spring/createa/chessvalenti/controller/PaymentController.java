package org.spring.createa.chessvalenti.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.PaymentConfirmRequest;
import org.spring.createa.chessvalenti.dto.UserPrincipal;
import org.spring.createa.chessvalenti.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/payment")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/confirm")
  public ResponseEntity<Map<String, Object>> confirmPayment(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentConfirmRequest body) {
    log.info("Payment confirmation request: {}", body);
    Map<String, Object> result = paymentService.confirmPayment(userPrincipal.getUser(), body);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/success")
  public String success(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
    model.addAttribute("username", userPrincipal.getUsername());
    return "success";
  }

  @GetMapping("/fail")
  public String fail() {
    return "fail";
  }

  @PostMapping("/refund/request")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refund(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody int id) {
    paymentService.requestRefund(userPrincipal.getUser(), id);
  }

  @PostMapping("/refund/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rejectRefund(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody int id) {
    paymentService.rejectRefund(id);
  }

  @PostMapping("/refund/accept")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void acceptRefund(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody int id) {
    paymentService.acceptRefund(id);
  }
}
