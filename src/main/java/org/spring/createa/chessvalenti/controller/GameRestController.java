package org.spring.createa.chessvalenti.controller;

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

@RestController
@Slf4j
@RequiredArgsConstructor
public class GameRestController {

    private final GameService gameService;
    private final ChessBoardService chessBoardService;

    @GetMapping("/board")
    public BoardResponse board(@RequestParam(required = false) String fen,
                               @RequestParam(required = false) String san,
                               @RequestParam(required = false) boolean ignoreLegalMove) {
        return chessBoardService.getBoardState(fen, san, ignoreLegalMove);
    }

    @GetMapping(value = "/api/games", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<GameInfo> searchGamesByPawnStructure(@RequestParam String fen,
                                                     @RequestParam(required = false) Integer whiteQueen,
                                                     @RequestParam(required = false) Integer whiteRook,
                                                     @RequestParam(required = false) Integer whiteBishop,
                                                     @RequestParam(required = false) Integer whiteKnight,
                                                     @RequestParam(required = false) Integer blackQueen,
                                                     @RequestParam(required = false) Integer blackRook,
                                                     @RequestParam(required = false) Integer blackBishop,
                                                     @RequestParam(required = false) Integer blackKnight,
                                                     @RequestParam(defaultValue = "false") boolean usePieceFilter) {
        log.info("Searching games by pawn structure: {}", fen);
        if (usePieceFilter) {
            return gameService.findGamesByPawnStructureAndPieceConfiguration(fen, whiteQueen,
                            whiteRook, whiteBishop, whiteKnight, blackQueen, blackRook, blackBishop, blackKnight)
                    .delayElements(Duration.ofMillis(1));
        }
        return gameService.findGamesByPawnStructure(fen).delayElements(Duration.ofMillis(1));
    }

    @GetMapping(value = "/api/games/paginated")
    public Page<GameInfo> searchGamesByPawnStructurePaginated(@RequestParam String fen,
                                                              @RequestParam(required = false) Integer whiteQueen,
                                                              @RequestParam(required = false) Integer whiteRook,
                                                              @RequestParam(required = false) Integer whiteBishop,
                                                              @RequestParam(required = false) Integer whiteKnight,
                                                              @RequestParam(required = false) Integer blackQueen,
                                                              @RequestParam(required = false) Integer blackRook,
                                                              @RequestParam(required = false) Integer blackBishop,
                                                              @RequestParam(required = false) Integer blackKnight,
                                                              @RequestParam(defaultValue = "false") boolean usePieceFilter,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "15") int size) {
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
