package org.spring.createa.chessvalenti.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.bhlangonijr.chesslib.Side;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.Insight;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.insight.PawnKey;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.dto.response.InsightProgressResponse;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

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

  private static final String SESSION = "session-1";

  private final JobHandle handle = new JobHandle();
  private final InsightGame game = new InsightGame("white", "[Event \"?\"]\n\n1. e4 e5 1-0",
      "user", "opponent", "standard");
  private final User user = new User();

  @BeforeEach
  void setUp() {
    // 작업 스레드 대신 호출한 스레드에서 바로 실행해 결과를 동기적으로 검증
    lenient().doAnswer(invocation -> {
      Consumer<JobHandle> task = invocation.getArgument(2);
      task.accept(handle);
      return null;
    }).when(jobService).work(anyLong(), eq(SESSION), any());
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

    insightService.createInsight(request, user, SESSION);

    verify(insightGameProcessor).loadGame(eq(game), eq("user"), anyMap());
    verify(insightGameProcessor).filterSimilarGame(anyMap());
    verify(simpMessagingTemplate, atLeastOnce()).convertAndSendToUser(eq(SESSION),
        eq("/queue/insight"), any(Object.class), anyMap());
    verify(insightRepository).save(any());
  }

  @Test
  void createInsight_WhenSaveFails_ShouldSendError() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    when(insightRepository.findByUser(any())).thenThrow(new IllegalStateException("db down"));

    insightService.createInsight(request, user, SESSION);

    verify(simpMessagingTemplate).convertAndSendToUser(eq(SESSION), eq("/queue/insight"),
        argThat((Object payload) -> isError(payload, "분석 중 오류")), anyMap());
  }

  @Test
  void createInsight_WhenUserNotFound_ShouldSendErrorAndSkipSave() {
    InsightRequestMessage request = new InsightRequestMessage("nobody", "blitz", "123456789",
        false, null);
    doAnswer(invocation -> {
      throw new UserNotFoundException("not found");
    }).when(lichessService).loadGames(eq("nobody"), any(), any(), any(), any());

    insightService.createInsight(request, user, SESSION);

    verify(simpMessagingTemplate).convertAndSendToUser(eq(SESSION), eq("/queue/insight"),
        argThat((Object payload) -> isError(payload, "존재하지 않는 사용자")), anyMap());
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

    insightService.createInsight(request, user, SESSION);

    verify(insightGameProcessor).loadGame(eq(game), eq("user"), anyMap());
    verify(insightRepository).save(any());
    verify(simpMessagingTemplate, never()).convertAndSendToUser(eq(SESSION), eq("/queue/insight"),
        argThat((Object payload) -> payload instanceof InsightProgressResponse response
            && "error".equals(response.status())), anyMap());
  }

  @Test
  void createInsight_WhenDisconnectedWhileLoading_ShouldNotSaveOrSend() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    doAnswer(invocation -> {
      emit(invocation.getArgument(4), game);
      handle.cancelAndDiscard();
      throw new IllegalStateException("stream closed");
    }).when(lichessService).loadGames(any(), any(), any(), any(), any());

    insightService.createInsight(request, user, SESSION);

    verify(insightGameProcessor, never()).filterSimilarGame(anyMap());
    verify(insightRepository, never()).save(any());
    verify(simpMessagingTemplate, never()).convertAndSendToUser(eq(SESSION), eq("/queue/insight"),
        argThat((Object payload) -> payload instanceof InsightProgressResponse response
            && "done".equals(response.status()) && !"register".equals(response.goal())),
        anyMap());
  }

  @Test
  void createInsight_WhenRejected_ShouldSendError() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    doAnswer(invocation -> {
      throw new RuntimeException("현재 분석 요청이 많습니다.");
    }).when(jobService).work(anyLong(), eq(SESSION), any());

    insightService.createInsight(request, user, SESSION);

    verify(simpMessagingTemplate).convertAndSendToUser(eq(SESSION), eq("/queue/insight"),
        argThat((Object payload) -> isError(payload, "요청이 많습니다")), anyMap());
  }

  @Test
  void createInsight_WithCancel_ShouldDisposeJob() {
    InsightRequestMessage cancel = new InsightRequestMessage("user", null, null, true, 7L);

    insightService.createInsight(cancel, user, SESSION);

    verify(jobService).dispose(7L, SESSION);
    verify(jobService, never()).work(anyLong(), any(), any());
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

    insightService.createInsight(request, user, SESSION);

    verify(insightGameProcessor).loadGame(eq(chessComGame), eq("user"), anyMap());
    verify(lichessService, never()).loadGames(any(), any(), any(), any(), any());
    verify(insightRepository).save(any());
  }

  @Test
  void createInsight_ShouldThrottleProgressAndSendFullResultOnce() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    doAnswer(invocation -> {
      for (int i = 0; i < 1000; i++) {
        emit(invocation.getArgument(4), game);
      }
      return null;
    }).when(lichessService).loadGames(any(), any(), any(), any(), any());
    when(insightRepository.findByUser(any())).thenReturn(Optional.empty());

    insightService.createInsight(request, user, SESSION);

    ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
    verify(simpMessagingTemplate, atLeastOnce()).convertAndSendToUser(eq(SESSION),
        eq("/queue/insight"), payloads.capture(), anyMap());
    List<InsightProgressResponse> responses = payloads.getAllValues().stream()
        .map(InsightProgressResponse.class::cast)
        .toList();

    long loadProgress = responses.stream()
        .filter(r -> "pending".equals(r.status()) && "load".equals(r.goal()))
        .count();
    assertThat(loadProgress).isLessThan(10);
    assertThat(responses).filteredOn(r -> r.data() != null).hasSize(1)
        .allMatch(r -> "filter".equals(r.goal()) && "done".equals(r.status()));
    assertThat(responses).filteredOn(r -> "load".equals(r.goal()) && "done".equals(r.status()))
        .singleElement().extracting(InsightProgressResponse::structureCount).isNotNull();
    verify(simpMessagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
  }

  @Test
  void createInsight_ShouldSendAndSaveOnlyTopStructures() {
    InsightRequestMessage request = new InsightRequestMessage("user", "blitz", "123456789", false,
        null);
    doAnswer(invocation -> {
      emit(invocation.getArgument(4), game);
      return null;
    }).when(lichessService).loadGames(any(), any(), any(), any(), any());
    // 한 게임에서 서로 다른 폰 구조 30개가 나온 것처럼 채움
    doAnswer(invocation -> {
      Map<PawnKey, GameResults> result = invocation.getArgument(2);
      for (int i = 0; i < 30; i++) {
        GameResults results = new GameResults();
        results.addResult(Side.WHITE, "white");
        result.put(new PawnKey(1L << (i + 8), 0L), results);
      }
      return null;
    }).when(insightGameProcessor).loadGame(any(), any(), anyMap());
    when(insightRepository.findByUser(any())).thenReturn(Optional.empty());

    insightService.createInsight(request, user, SESSION);

    ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
    verify(simpMessagingTemplate, atLeastOnce()).convertAndSendToUser(eq(SESSION),
        eq("/queue/insight"), payloads.capture(), anyMap());
    InsightProgressResponse filterDone = payloads.getAllValues().stream()
        .map(InsightProgressResponse.class::cast)
        .filter(r -> "filter".equals(r.goal()) && "done".equals(r.status()))
        .findFirst().orElseThrow();
    assertThat(filterDone.structureCount()).isEqualTo(30);
    assertThat(filterDone.data()).hasSizeLessThanOrEqualTo(16)
        .allSatisfy((fen, results) -> assertThat(fen).endsWith(" w"));

    ArgumentCaptor<Insight> saved = ArgumentCaptor.forClass(Insight.class);
    verify(insightRepository).save(saved.capture());
    assertThat(saved.getValue().getData()).hasSizeLessThanOrEqualTo(16);
  }

  @Test
  void onDisconnect_ShouldCancelJobsOfThatSession() {
    SessionDisconnectEvent event = new SessionDisconnectEvent(this,
        MessageBuilder.withPayload(new byte[0]).build(), SESSION, CloseStatus.NORMAL);

    insightService.onDisconnect(event);

    verify(jobService).disposeAllOf(SESSION);
  }
}
