package org.spring.createa.chessvalenti.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
public class InsightServiceTest {

  @Mock
  private SimpMessagingTemplate simpMessagingTemplate;

  @Mock
  private LichessService lichessService;

  @Mock
  private ChessComService chessComService;

  @Mock
  private InsightGameProcessor insightGameProcessor;

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
    InsightGame game = new InsightGame("white", "[Event \"?\"]\n\n1. e4 e5 1-0", "user",
        "opponent", "standard");
    org.spring.createa.chessvalenti.domain.User user = new org.spring.createa.chessvalenti.domain.User();

    when(lichessService.loadGames(eq("user"), eq("blitz"), eq("123456789")))
        .thenReturn(Flux.just(game));
    when(insightRepository.findByUser(any())).thenReturn(java.util.Optional.empty());

    insightService.createInsight(request, user);

    verify(lichessService).loadGames(eq("user"), eq("blitz"), eq("123456789"));
    verify(insightGameProcessor).loadGame(any(InsightGame.class), eq("user"), anyMap());
    verify(simpMessagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/insight"),
        any(Object.class));
    verify(jobService).work(any(), eq(0L));
    verify(insightRepository).save(any());
  }

  @Test
  void createInsight_WithCancel_ShouldDisposeJob() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false, 0L);
    InsightGame game = new InsightGame("white", "[Event \"?\"]\n\n1. e4 e5 1-0", "user",
        "opponent", "standard");
    org.spring.createa.chessvalenti.domain.User user = new org.spring.createa.chessvalenti.domain.User();

    when(lichessService.loadGames(eq("user"), eq("blitz"), eq("123456789")))
        .thenReturn(Flux.just(game).delayElements(Duration.ofSeconds(5)));
    
    insightService.createInsight(request, user);
    
    InsightRequestMessage request_cancel = new InsightRequestMessage("user", null, null, true, 0L);
    insightService.createInsight(request_cancel, user);
    
    verify(jobService).work(any(), eq(0L));
    verify(jobService).dispose(0L);
  }

  @Test
  void createInsight_WithChessComPlatform_ShouldLoadChessComGames() {
    InsightRequestMessage request = new InsightRequestMessage("user", "rapid", "123456789", false,
        null, "chesscom");
    InsightGame game = new InsightGame("white", "[Event \"?\"]\n\n1. e4 e5 1-0", "user",
        "opponent", "chess");
    org.spring.createa.chessvalenti.domain.User user = new org.spring.createa.chessvalenti.domain.User();

    when(chessComService.loadGames(eq("user"), eq("rapid"), eq("123456789")))
        .thenReturn(Flux.just(game));
    when(insightRepository.findByUser(any())).thenReturn(java.util.Optional.empty());

    insightService.createInsight(request, user);

    verify(chessComService).loadGames(eq("user"), eq("rapid"), eq("123456789"));
    verify(insightGameProcessor).loadGame(eq(game), eq("user"), anyMap());
    verify(jobService).work(any(), eq(0L));
    verify(insightRepository).save(any());
  }
}
