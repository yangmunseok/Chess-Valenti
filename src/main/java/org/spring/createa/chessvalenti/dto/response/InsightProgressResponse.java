package org.spring.createa.chessvalenti.dto.response;

import java.util.Map;
import org.spring.createa.chessvalenti.dto.game.GameResults;

public record InsightProgressResponse(String username, int loadedGame, String status, String goal,
                             Long jobId,
                             Map<String, GameResults> data) {

}
