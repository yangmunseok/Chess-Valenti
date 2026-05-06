package org.spring.createa.chessvalenti.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import org.apache.tomcat.util.json.JSONParser;
import org.spring.createa.chessvalenti.db.PaymentRepository;
import org.spring.createa.chessvalenti.domain.Payment;
import org.spring.createa.chessvalenti.domain.PaymentState;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.PaymentConfirmRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

  @Value("${toss.payment.secret-key}")
  private String widgetSecretKey;

  private final UserService userService;
  PaymentRepository paymentRepository;

  public PaymentService(PaymentRepository paymentRepository, UserService userService) {
    this.paymentRepository = paymentRepository;
    this.userService = userService;
  }

  public Object confirmPayment(PaymentConfirmRequest body) throws Exception {
    // 토스페이먼츠 API는 시크릿 키를 사용자 ID로 사용하고, 비밀번호는 사용하지 않습니다.
    // 비밀번호가 없다는 것을 알리기 위해 시크릿 키 뒤에 콜론을 추가합니다.
    Base64.Encoder encoder = Base64.getEncoder();
    byte[] encodedBytes = encoder.encode(
        (widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
    String authorizations = "Basic " + new String(encodedBytes);

    // 결제를 승인하면 결제수단에서 금액이 차감돼요.
    URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty("Authorization", authorizations);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);

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
    return parser.parse();
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
