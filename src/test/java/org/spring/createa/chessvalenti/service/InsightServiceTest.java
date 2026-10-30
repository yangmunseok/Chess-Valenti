package org.spring.createa.chessvalenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bhlangonijr.chesslib.Side;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.Insight;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.request.InsightSaveRequest;

@ExtendWith(MockitoExtension.class)
public class InsightServiceTest {

  private static final String FEN = "8/pp3ppp/8/2ppp3/2PPP3/8/PP3PPP/8 w";

  @Mock
  private InsightRepository insightRepository;

  @InjectMocks
  private InsightService insightService;

  private final User user = new User();

  private static GameResults wins(int count) {
    GameResults results = new GameResults();
    for (int i = 0; i < count; i++) {
      results.addResult(Side.WHITE, "white");
    }
    return results;
  }

  private static InsightSaveRequest saveRequest(String fen, GameResults results) {
    return new InsightSaveRequest("Hikaru", "blitz", 1700000000000L, Map.of(fen, results));
  }

  @Test
  void save_ShouldOverwriteTheUsersInsight() {
    Insight existing = new Insight();
    when(insightRepository.findByUser(user)).thenReturn(Optional.of(existing));

    insightService.save(user, saveRequest(FEN, wins(3)));

    verify(insightRepository).save(existing);
    assertThat(existing.getUser()).isSameAs(user);
    assertThat(existing.getLichessUsername()).isEqualTo("Hikaru");
    assertThat(existing.getPerfType()).isEqualTo("blitz");
    assertThat(existing.getSince()).isEqualTo(1700000000000L);
    assertThat(existing.getData()).containsOnlyKeys(FEN);
  }

  @Test
  void save_WithoutPreviousInsight_ShouldCreateOne() {
    when(insightRepository.findByUser(user)).thenReturn(Optional.empty());

    insightService.save(user, saveRequest(FEN, wins(1)));

    verify(insightRepository).save(any(Insight.class));
  }

  @Test
  void save_WithMalformedFen_ShouldReject() {
    for (String fen : List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w",
        "8/pppppppp/8/8/8/8/PPPPPPPP/8", "8/ppppppppp/8/8/8/8/PPPPPPPP/8 w",
        "8/pppppppp/8/8/8/8/PPPPPPP/8 w")) {
      assertThatThrownBy(() -> insightService.save(user, saveRequest(fen, wins(1))))
          .as(fen).isInstanceOf(IllegalArgumentException.class);
    }
    verify(insightRepository, never()).save(any());
  }

  @Test
  void save_WithNegativeCounts_ShouldReject() throws Exception {
    GameResults negative = new ObjectMapper().readValue(
        "{\"white\":{\"whiteWon\":-5,\"blackWon\":0,\"drawn\":0}}", GameResults.class);

    assertThatThrownBy(() -> insightService.save(user, saveRequest(FEN, negative)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(insightRepository, never()).save(any());
  }
}
