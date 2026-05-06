package org.spring.createa.chessvalenti.dto;

import java.util.List;

public record BoardResponse(String fen, List<String> legalMove, boolean isKingAttacked,
                            boolean isMated) {

}
