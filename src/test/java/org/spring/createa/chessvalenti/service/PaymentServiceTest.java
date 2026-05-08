package org.spring.createa.chessvalenti.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.db.PaymentRepository;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.request.PaymentConfirmRequest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private UserService userService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebClient webClient;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        userService = new TestUserService();
        paymentService = new PaymentService(userService, paymentRepository, webClient);
        ReflectionTestUtils.setField(paymentService, "widgetSecretKey", "test_key");
    }

    @Test
    void confirmPayment_Success() {
        // Given
        User user = new User("test@test.com", "testuser", "password", Role.ROLE_USER);
        PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "1000", "orderId");
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("status", "DONE");

        // Mocking WebClient chain
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        // When
        Map<String, Object> result = paymentService.confirmPayment(user, request);

        // Then
        assertNotNull(result);
        assertEquals("DONE", result.get("status"));
        
        // Verify interactions
        verify(paymentRepository, times(1)).save(any());
        assertEquals(1000, user.getDonation());
    }

    static class TestUserService extends UserService {

        TestUserService() {
            super(mock(UserRepository.class), mock(SessionRegistry.class));
        }

        @Override
        public void addDonation(User user, int amount) {
            user.setDonation(user.getDonation() + amount);
        }
    }
}
