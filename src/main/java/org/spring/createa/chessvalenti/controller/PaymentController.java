package org.spring.createa.chessvalenti.controller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.tomcat.util.json.JSONParser;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.PaymentConfirmRequest;
import org.spring.createa.chessvalenti.dto.UserPrincipal;
import org.spring.createa.chessvalenti.service.PaymentService;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.beans.factory.annotation.Value;
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
public class PaymentController {

  @Value("${toss.payment.secret-key}")
  private String widgetSecretKey;

  private final PaymentService paymentService;
  private final UserService userService;

  public PaymentController(PaymentService paymentService, UserService userService) {
    this.paymentService = paymentService;
    this.userService = userService;
  }

  @PostMapping("/confirm")
  public ResponseEntity<Object> confirmPayment(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentConfirmRequest body)
      throws Exception {
    // 토스페이먼츠 API는 시크릿 키를 사용자 ID로 사용하고, 비밀번호는 사용하지 않습니다.
    // 비밀번호가 없다는 것을 알리기 위해 시크릿 키 뒤에 콜론을 추가합니다.
    try {
      Base64.Encoder encoder = Base64.getEncoder();
      byte[] encodedBytes = encoder.encode(
          (widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
      String authorizations = "Basic " + new String(encodedBytes);

      // 결제를 승인하면 결제수단에서 금액이 차감돼요.
      URI uri = new URI("https://api.tosspayments.com/v1/payments/confirm");
      URL url = uri.toURL();
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("Authorization", authorizations);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);

      System.out.println(body);

      OutputStream outputStream = connection.getOutputStream();
      outputStream.write(body.toString().getBytes(StandardCharsets.UTF_8));

      int code = connection.getResponseCode();
      boolean isSuccess = code == 200;

      InputStream responseStream =
          isSuccess ? connection.getInputStream() : connection.getErrorStream();

      // 결제 성공 및 실패 비즈니스 로직을 구현하세요.
      Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8);
      JSONParser parser = new JSONParser(reader);
      responseStream.close();
      Object obj = parser.parse();
      System.out.println(obj);
      if (isSuccess) {
        User user = userPrincipal.getUser();
        int amount = Integer.parseInt(body.amount());
        paymentService.savePayment(user, amount, "toss",
            "donation");
        userService.addDonation(user, amount);
      }
      return ResponseEntity.status(200).body(obj);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }

  }

  @GetMapping("/success")
  public String success(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model)
      throws Exception {
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
