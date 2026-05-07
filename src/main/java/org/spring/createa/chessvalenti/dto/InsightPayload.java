package org.spring.createa.chessvalenti.dto;

import java.util.Map;

public record InsightPayload(String username, int loadedGame, String status, String goal,
                             Long jobId,
                             Map<String, GameResults> data) {

}
