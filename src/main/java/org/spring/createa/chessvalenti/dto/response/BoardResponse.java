package org.spring.createa.chessvalenti.dto.response;

import java.util.List;

public record BoardResponse(String fen, List<String> legalMove, boolean isKingAttacked,
                            boolean isMated, java.util.List<String> uciMoves) {

}
