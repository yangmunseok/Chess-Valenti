package org.spring.createa.chessvalenti.dto.response;

import java.util.Map;
import org.spring.createa.chessvalenti.dto.game.GameResults;

/**
 * @param structureCount 불러오기가 끝났을 때 모인 폰 구조 수. 결과 전체(data)는 마지막에 한 번만 보낸다.
 */
public record InsightProgressResponse(String systemUsername, String username, int loadedGame, String status, String goal,
                             Long jobId,
                             Map<String, GameResults> data,
                             Integer structureCount) {

}
