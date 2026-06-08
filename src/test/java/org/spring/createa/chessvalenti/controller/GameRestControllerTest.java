package org.spring.createa.chessvalenti.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.service.ChessBoardService;
import org.spring.createa.chessvalenti.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

@WebMvcTest(GameRestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GameRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private ChessBoardService chessBoardService;

    @Test
    void board_ShouldReturnBoardResponse() throws Exception {
        mockMvc.perform(get("/board")
                        .param("fen", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
                .andExpect(status().isOk());
    }

    @Test
    void searchGames_ShouldReturnNDJSON() throws Exception {
        when(gameService.findGamesByPawnStructure(anyString())).thenReturn(Flux.empty());

        mockMvc.perform(get("/api/games")
                        .param("fen", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_NDJSON_VALUE));
    }
}
