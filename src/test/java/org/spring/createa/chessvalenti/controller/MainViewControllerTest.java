package org.spring.createa.chessvalenti.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MainViewController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MainViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private PostService postService;

    @MockBean
    private InquiryService inquiryService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private ChessBoardService chessBoardService;

    @MockBean
    private InsightRepository insightRepository;

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
    void analysis_ShouldReturnAnalysisView() throws Exception {
        mockMvc.perform(get("/analysis"))
                .andExpect(status().isOk())
                .andExpect(view().name("analysis"));
    }
}
