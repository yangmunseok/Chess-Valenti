package org.spring.createa.chessvalenti.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
public class InsightServiceTest {

  @Mock
  private SimpMessagingTemplate simpMessagingTemplate;

  @Mock
  private LichessService lichessService;

  @Mock
  private LichessApi lichessApi;

  @Mock
  private org.spring.createa.chessvalenti.db.InsightRepository insightRepository;

  @Spy
  private JobService jobService;

  @InjectMocks
  private InsightService insightService;

  @Test
  void createInsight_ShouldProcessGamesAndSendProgress() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    LichessGameResponse response = new LichessGameResponse(null, null, null, null, null);
    org.spring.createa.chessvalenti.domain.User user = new org.spring.createa.chessvalenti.domain.User();

    when(lichessApi.loadGames(eq("user"), eq(true), eq("blitz"), eq("123456789")))
        .thenReturn(Flux.just(response));
    when(insightRepository.findByUser(any())).thenReturn(java.util.Optional.empty());

    insightService.createInsight(request, user);

    verify(lichessApi).loadGames(anyString(), anyBoolean(), anyString(), anyString());
    verify(lichessService).loadGame(eq(response), eq("user"), anyMap());
    verify(simpMessagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/insight"),
        any(Object.class));
    verify(jobService).work(any(), eq(0L));
    verify(insightRepository).save(any());
  }

  @Test
  void createInsight_WithCancel_ShouldDisposeJob() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false, 0L);
    LichessGameResponse response = new LichessGameResponse(null, null, null, null, null);
    org.spring.createa.chessvalenti.domain.User user = new org.spring.createa.chessvalenti.domain.User();

    when(lichessApi.loadGames(eq("user"), eq(true), eq("blitz"), eq("123456789")))
        .thenReturn(Flux.just(response).delayElements(Duration.ofSeconds(5)));
    
    insightService.createInsight(request, user);
    
    InsightRequestMessage request_cancel = new InsightRequestMessage("user", null, null, true, 0L);
    insightService.createInsight(request_cancel, user);
    
    verify(jobService).work(any(), eq(0L));
    verify(jobService).dispose(0L);
  }
}
