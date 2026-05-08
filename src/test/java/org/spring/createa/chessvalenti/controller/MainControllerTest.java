package org.spring.createa.chessvalenti.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.dto.game.GameInfo;
import org.spring.createa.chessvalenti.service.GameService;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.spring.createa.chessvalenti.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

@WebMvcTest(MainController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MainControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GameService gameService;

  @MockBean
  private PostService postService;

  @MockBean
  private InquiryService inquiryService;

  @Test
  void index_ShouldReturnIndexView() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"));
  }

  @Test
  @WithMockUser(username = "testuser")
  void home_ShouldReturnHomeView() throws Exception {
    when(postService.findAllByPostType(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));
    when(postService.findFAQ()).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("username", "testuser"));
  }

  @Test
  void board_ShouldReturnBoardResponse() throws Exception {
    mockMvc.perform(get("/board")
            .param("fen", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fen").exists())
        .andExpect(jsonPath("$.legalMove").isArray());
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
