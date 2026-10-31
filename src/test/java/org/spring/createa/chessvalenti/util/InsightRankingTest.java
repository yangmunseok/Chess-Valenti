package org.spring.createa.chessvalenti.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.bhlangonijr.chesslib.Side;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.dto.game.GameResults;

class InsightRankingTest {

  // 흰색으로 둔 won 승, lost 패, drawn 무
  private static GameResults results(int won, int lost, int drawn) {
    GameResults results = new GameResults();
    for (int i = 0; i < won; i++) {
      results.addResult(Side.WHITE, "white");
    }
    for (int i = 0; i < lost; i++) {
      results.addResult(Side.WHITE, "black");
    }
    for (int i = 0; i < drawn; i++) {
      results.addResult(Side.WHITE, "");
    }
    return results;
  }

  @Test
  void selectTop_ShouldKeepAtMostSixteen() {
    Map<String, GameResults> map = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      map.put("s" + i, results(i % 7, i % 5, i % 3 + 8));
    }

    assertThat(InsightRanking.selectTop(map)).hasSizeLessThanOrEqualTo(16);
  }

  @Test
  void selectTop_ShouldKeepEachListsTopStructures() {
    Map<String, GameResults> map = new HashMap<>();
    for (int i = 0; i < 20; i++) {
      map.put("filler" + i, results(5, 5, 2)); // 12판, 승률·패배율 중간
    }
    map.put("mostPlayed", results(20, 20, 20));
    map.put("bestWinRate", results(11, 0, 0));
    map.put("worstLossRate", results(0, 11, 0));
    map.put("fewGamesAllWins", results(3, 0, 0));

    Map<String, GameResults> top = InsightRanking.selectTop(map);

    assertThat(top).containsKeys("mostPlayed", "bestWinRate", "worstLossRate");
    // 10판 이하라 승률 목록에 못 들어가고, 판 수도 적어 많이 둔 목록에도 못 든다
    assertThat(top).doesNotContainKey("fewGamesAllWins");
  }

  @Test
  void selectTop_WithFewStructures_ShouldKeepAll() {
    Map<String, GameResults> map = new HashMap<>();
    map.put("a", results(1, 0, 0));
    map.put("b", results(0, 1, 0));

    assertThat(InsightRanking.selectTop(map)).containsOnlyKeys("a", "b");
  }
}
