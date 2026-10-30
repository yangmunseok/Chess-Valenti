package org.spring.createa.chessvalenti.dto.response;

import java.util.Map;
import org.spring.createa.chessvalenti.dto.game.GameResults;

/**
 * @param structureCount 불러오기 완료 시에는 모인 폰 구조 수, 필터링 완료 시에는 필터링 후 남은 구조 수.
 * @param data           필터링 완료 시 한 번만 보낸다. 화면에 보여 줄 상위 구조만 담는다.
 */
public record InsightProgressResponse(String systemUsername, String username, int loadedGame, String status, String goal,
                             Long jobId,
                             Map<String, GameResults> data,
                             Integer structureCount) {

}
