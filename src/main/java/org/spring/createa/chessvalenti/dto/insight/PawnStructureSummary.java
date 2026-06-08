package org.spring.createa.chessvalenti.dto.insight;

public record PawnStructureSummary(
    long whitePawns,
    long blackPawns,
    int whiteDoubled,
    int blackDoubled,
    int whiteIsolated,
    int blackIsolated,
    int totalPawns
) {
}
