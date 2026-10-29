package org.spring.createa.chessvalenti.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.dto.response.InsightProgressResponse;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
  private InsightRepository insightRepository;

  @Mock
  private JobService jobService;

  @InjectMocks
  private InsightService insightService;

  private final JobHandle handle = new JobHandle();
  private final InsightGame game = new InsightGame("white", "[Event \"?\"]\n\n1. e4 e5 1-0",
      "user", "opponent", "standard");
  private final User user = new User();

  @BeforeEach
  void setUp() {
    // 작업 스레드 대신 호출한 스레드에서 바로 실행해 결과를 동기적으로 검증
    lenient().doAnswer(invocation -> {
      Consumer<JobHandle> task = invocation.getArgument(1);
      task.accept(handle);
      return null;
    }).when(jobService).work(anyLong(), any());
  }

  @SuppressWarnings("unchecked")
  private static void emit(Object consumer, InsightGame game) {
    ((Consumer<InsightGame>) consumer).accept(game);
  }

  private static boolean isError(Object payload, String message) {
    return payload instanceof InsightProgressResponse response
        && "error".equals(response.status())
        && response.goal() != null && response.goal().contains(message);
  }

  @Test
  void createInsight_ShouldProcessGamesAndSendProgress() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    doAnswer(invocation -> {
      emit(invocation.getArgument(4), game);
      return null;
    }).when(lichessService).loadGames(eq("user"), eq("blitz"), eq("123456789"), any(), any());
    when(insightRepository.findByUser(any())).thenReturn(Optional.empty());

    insightService.createInsight(request, user);

    verify(insightGameProcessor).loadGame(eq(game), eq("user"), anyMap());
    verify(insightGameProcessor).filterSimilarGame(anyMap());
    verify(simpMessagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/insight"),
        any(Object.class));
    verify(insightRepository).save(any());
  }

  @Test
  void createInsight_WhenSaveFails_ShouldSendError() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    when(insightRepository.findByUser(any())).thenThrow(new IllegalStateException("db down"));

    insightService.createInsight(request, user);

    verify(simpMessagingTemplate).convertAndSend(eq("/topic/insight"),
        argThat((Object payload) -> isError(payload, "분석 중 오류")));
  }

  @Test
  void createInsight_WhenUserNotFound_ShouldSendErrorAndSkipSave() {
    InsightRequestMessage request = new InsightRequestMessage("nobody", "blitz", "123456789",
        false, null);
    doAnswer(invocation -> {
      throw new UserNotFoundException("not found");
    }).when(lichessService).loadGames(eq("nobody"), any(), any(), any(), any());

    insightService.createInsight(request, user);

    verify(simpMessagingTemplate).convertAndSend(eq("/topic/insight"),
        argThat((Object payload) -> isError(payload, "존재하지 않는 사용자")));
    verify(insightRepository, never()).save(any());
  }

  @Test
  void createInsight_WhenCancelledWhileLoading_ShouldSavePartialResult() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    doAnswer(invocation -> {
      emit(invocation.getArgument(4), game);
      // 취소로 스트림이 닫히면 읽기 중이던 코드는 예외로 빠져나온다
      handle.cancel();
      throw new IllegalStateException("stream closed");
    }).when(lichessService).loadGames(any(), any(), any(), any(), any());
    when(insightRepository.findByUser(any())).thenReturn(Optional.empty());

    insightService.createInsight(request, user);

    verify(insightGameProcessor).loadGame(eq(game), eq("user"), anyMap());
    verify(insightRepository).save(any());
    verify(simpMessagingTemplate, never()).convertAndSend(eq("/topic/insight"),
        argThat((Object payload) -> payload instanceof InsightProgressResponse response
            && "error".equals(response.status())));
  }

  @Test
  void createInsight_WhenRejected_ShouldSendError() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    doAnswer(invocation -> {
      throw new RuntimeException("현재 분석 요청이 많습니다.");
    }).when(jobService).work(anyLong(), any());

    insightService.createInsight(request, user);

    verify(simpMessagingTemplate).convertAndSend(eq("/topic/insight"),
        argThat((Object payload) -> isError(payload, "요청이 많습니다")));
  }

  @Test
  void createInsight_WithCancel_ShouldDisposeJob() {
    InsightRequestMessage cancel = new InsightRequestMessage("user", null, null, true, 7L);

    insightService.createInsight(cancel, user);

    verify(jobService).dispose(7L);
    verify(jobService, never()).work(anyLong(), any());
  }

  @Test
  void createInsight_WithChessComPlatform_ShouldLoadChessComGames() {
    InsightRequestMessage request = new InsightRequestMessage("user", "rapid", "123456789", false,
        null, "chesscom");
    InsightGame chessComGame = new InsightGame("white", "[Event \"?\"]\n\n1. e4 e5 1-0", "user",
        "opponent", "chess");
    doAnswer(invocation -> {
      emit(invocation.getArgument(4), chessComGame);
      return null;
    }).when(chessComService).loadGames(eq("user"), eq("rapid"), eq("123456789"), any(), any());
    when(insightRepository.findByUser(any())).thenReturn(Optional.empty());

    insightService.createInsight(request, user);

    verify(insightGameProcessor).loadGame(eq(chessComGame), eq("user"), anyMap());
    verify(lichessService, never()).loadGames(any(), any(), any(), any(), any());
    verify(insightRepository).save(any());
  }
}
