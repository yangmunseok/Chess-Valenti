package org.spring.createa.chessvalenti.service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.Insight;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.game.PlayerColor;
import org.spring.createa.chessvalenti.dto.request.InsightSaveRequest;
import org.springframework.stereotype.Service;

/**
 * 인사이트 결과를 저장한다. 게임을 받아 분석하는 일은 브라우저(insightAnalysis.js)가 한다.
 */
@Service
@RequiredArgsConstructor
public class InsightService {

  // 폰만 남긴 FEN: 8개 랭크(폰 P/p 와 빈칸 수 1~8) + " w"
  private static final Pattern PAWNS_ONLY_FEN = Pattern.compile("([pP1-8]{1,8}/){7}[pP1-8]{1,8} w");

  private final InsightRepository insightRepository;

  /**
   * 브라우저에서 계산한 결과를 사용자의 인사이트로 저장한다. 사용자당 하나이며 이전 결과를 덮어쓴다.
   */
  public void save(User user, InsightSaveRequest request) {
    request.data().forEach(InsightService::validateStructure);

    Insight insight = insightRepository.findByUser(user).orElse(new Insight());
    insight.setUser(user);
    insight.setLichessUsername(request.username());
    insight.setPerfType(request.perfType());
    insight.setSince(request.since());
    insight.setData(request.data());
    insight.setCreatedAt(LocalDateTime.now());
    insightRepository.save(insight);
  }

  private static void validateStructure(String fen, GameResults results) {
    if (fen == null || !PAWNS_ONLY_FEN.matcher(fen).matches() || !hasEightSquaresPerRank(fen)) {
      throw new IllegalArgumentException("잘못된 폰 구조입니다: " + fen);
    }
    if (results == null || !isValidCount(results.getWhite())
        || !isValidCount(results.getBlack())) {
      throw new IllegalArgumentException("잘못된 승패 기록입니다: " + fen);
    }
  }

  private static boolean hasEightSquaresPerRank(String fen) {
    for (String rank : fen.substring(0, fen.length() - 2).split("/")) {
      int squares = 0;
      for (char c : rank.toCharArray()) {
        squares += Character.isDigit(c) ? c - '0' : 1;
      }
      if (squares != 8) {
        return false;
      }
    }
    return true;
  }

  private static boolean isValidCount(PlayerColor color) {
    return color != null && color.getWhiteWon() >= 0 && color.getBlackWon() >= 0
        && color.getDrawn() >= 0;
  }
}
