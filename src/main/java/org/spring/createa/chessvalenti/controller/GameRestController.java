package org.spring.createa.chessvalenti.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.game.GameInfo;
import org.spring.createa.chessvalenti.dto.response.BoardResponse;
import org.spring.createa.chessvalenti.service.ChessBoardService;
import org.spring.createa.chessvalenti.service.GameService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Tag(name = "Game REST API", description = "체스 게임 및 보드 상태 조회 API")
@RestController
@Slf4j
@RequiredArgsConstructor
public class GameRestController {

    private final GameService gameService;
    private final ChessBoardService chessBoardService;

    @Operation(summary = "체스 보드 상태 조회", description = "FEN과 SAN을 기반으로 현재 체스 보드의 상태를 계산하여 반환합니다.")
    @GetMapping("/board")
    public BoardResponse board(
            @Parameter(description = "FEN (Forsyth-Edwards Notation) 문자열") @RequestParam(required = false) String fen,
            @Parameter(description = "SAN (Standard Algebraic Notation) 수 정보") @RequestParam(required = false) String san,
            @Parameter(description = "합법수 체크 무시 여부") @RequestParam(required = false) boolean ignoreLegalMove) {
        return chessBoardService.getBoardState(fen, san, ignoreLegalMove);
    }

    @Operation(summary = "폰 구조 기반 게임 검색 (Stream/NDJSON)", description = "폰 구조(FEN)와 기물 구성을 기반으로 게임 리스트를 NDJSON 스트림 형태로 검색합니다.")
    @GetMapping(value = "/api/games", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<GameInfo> searchGamesByPawnStructure(
            @Parameter(description = "기준이 되는 FEN 문자열") @RequestParam String fen,
            @Parameter(description = "백 퀸 개수") @RequestParam(required = false) Integer whiteQueen,
            @Parameter(description = "백 룩 개수") @RequestParam(required = false) Integer whiteRook,
            @Parameter(description = "백 비숍 개수") @RequestParam(required = false) Integer whiteBishop,
            @Parameter(description = "백 나이트 개수") @RequestParam(required = false) Integer whiteKnight,
            @Parameter(description = "흑 퀸 개수") @RequestParam(required = false) Integer blackQueen,
            @Parameter(description = "흑 룩 개수") @RequestParam(required = false) Integer blackRook,
            @Parameter(description = "흑 비숍 개수") @RequestParam(required = false) Integer blackBishop,
            @Parameter(description = "흑 나이트 개수") @RequestParam(required = false) Integer blackKnight,
            @Parameter(description = "기물 필터 사용 여부") @RequestParam(defaultValue = "false") boolean usePieceFilter) {
        log.info("Searching games by pawn structure: {}", fen);
        if (usePieceFilter) {
            return gameService.findGamesByPawnStructureAndPieceConfiguration(fen, whiteQueen,
                            whiteRook, whiteBishop, whiteKnight, blackQueen, blackRook, blackBishop, blackKnight)
                    .delayElements(Duration.ofMillis(1));
        }
        return gameService.findGamesByPawnStructure(fen).delayElements(Duration.ofMillis(1));
    }

    @Operation(summary = "폰 구조 기반 게임 검색 (페이징)", description = "폰 구조(FEN)와 기물 구성을 기반으로 게임 리스트를 페이지 단위로 검색합니다.")
    @GetMapping(value = "/api/games/paginated")
    public Page<GameInfo> searchGamesByPawnStructurePaginated(
            @Parameter(description = "기준이 되는 FEN 문자열") @RequestParam String fen,
            @Parameter(description = "백 퀸 개수") @RequestParam(required = false) Integer whiteQueen,
            @Parameter(description = "백 룩 개수") @RequestParam(required = false) Integer whiteRook,
            @Parameter(description = "백 비숍 개수") @RequestParam(required = false) Integer whiteBishop,
            @Parameter(description = "백 나이트 개수") @RequestParam(required = false) Integer whiteKnight,
            @Parameter(description = "흑 퀸 개수") @RequestParam(required = false) Integer blackQueen,
            @Parameter(description = "흑 룩 개수") @RequestParam(required = false) Integer blackRook,
            @Parameter(description = "흑 비숍 개수") @RequestParam(required = false) Integer blackBishop,
            @Parameter(description = "흑 나이트 개수") @RequestParam(required = false) Integer blackKnight,
            @Parameter(description = "기물 필터 사용 여부") @RequestParam(defaultValue = "false") boolean usePieceFilter,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "15") int size) {
        log.info("Searching paginated games by pawn structure: {}, page: {}, size: {}", fen, page, size);
        Pageable pageable = PageRequest.of(page, size);
        if (usePieceFilter) {
            return gameService.findGamesByPawnStructureAndPieceConfiguration(fen, whiteQueen,
                    whiteRook, whiteBishop, whiteKnight, blackQueen, blackRook, blackBishop, blackKnight,
                    pageable);
        }
        return gameService.findGamesByPawnStructure(fen, pageable);
    }
}
