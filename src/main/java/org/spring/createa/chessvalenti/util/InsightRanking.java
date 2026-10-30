package org.spring.createa.chessvalenti.util;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.spring.createa.chessvalenti.dto.game.GameResults;

/**
 * 저장된 인사이트 중 화면에 보여 줄 폰 구조만 고른다.
 *
 * <p>insight.js 의 renderResultPage 가 보여 주는 세 목록과 같은 기준이다(insightAnalysis.js 의 selectTop 과 같음).
 * 지금은 브라우저가 상위 구조만 저장하지만, 예전에는 전체 결과를 저장했으므로 그런 행을 페이지에 넘길 때 자른다.
 */
public final class InsightRanking {

  static final int TOP_FREQUENT = 5;
  static final int TOP_WIN_RATE = 5;
  static final int TOP_LOSS_RATE = 6;
  // 판 수가 적은 구조는 승률이 쉽게 0%나 100%가 되므로 승률 목록에서 뺀다
  static final int MIN_GAMES_FOR_RATE = 10;

  private InsightRanking() {
  }

  public static <K> Map<K, GameResults> selectTop(Map<K, GameResults> results) {
    Map<K, GameResults> top = new LinkedHashMap<>();
    Predicate<GameResults> enoughGames = r -> r.getTotal() > MIN_GAMES_FOR_RATE;

    pick(results, Comparator.comparingInt(GameResults::getTotal).reversed(), r -> true,
        TOP_FREQUENT, top);
    pick(results, Comparator.comparingDouble(InsightRanking::winRate).reversed(), enoughGames,
        TOP_WIN_RATE, top);
    pick(results, Comparator.comparingDouble(InsightRanking::lossRate).reversed(), enoughGames,
        TOP_LOSS_RATE, top);
    return top;
  }

  private static <K> void pick(Map<K, GameResults> results, Comparator<GameResults> order,
      Predicate<GameResults> filter, int limit, Map<K, GameResults> top) {
    results.entrySet().stream()
        .filter(entry -> filter.test(entry.getValue()))
        .sorted(Map.Entry.comparingByValue(order))
        .limit(limit)
        .forEach(entry -> top.put(entry.getKey(), entry.getValue()));
  }

  private static double winRate(GameResults results) {
    return (double) (results.getWhite().getWhiteWon() + results.getBlack().getBlackWon())
        / results.getTotal();
  }

  private static double lossRate(GameResults results) {
    return (double) (results.getBlack().getWhiteWon() + results.getWhite().getBlackWon())
        / results.getTotal();
  }
}
