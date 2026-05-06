package org.spring.createa.chessvalenti.dto;

import java.util.Map;
import org.spring.createa.chessvalenti.service.LichessService.GameResults;

public record InsightPayload(String username, int loadedGame, String status, String goal,
                             Long jobId,
                             Map<String, GameResults> data) {

}
