package org.spring.createa.chessvalenti.controller;

import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.domain.*;
import org.spring.createa.chessvalenti.dto.response.AdminUserStatsResponse;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.spring.createa.chessvalenti.service.PaymentService;
import org.spring.createa.chessvalenti.service.PostService;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminViewController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InquiryService inquiryService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private UserService userService;

    @MockBean
    private PostService postService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPage_ShouldReturnAdminView() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void userListPage_ShouldAddStatsToModel() throws Exception {
        AdminUserStatsResponse stats = new AdminUserStatsResponse(
                new PageImpl<>(Collections.emptyList()),
                Collections.emptyList(),
                0, 0, 0, 0.0, 0, 0
        );
        when(userService.getAdminUserStats(any(), any(), anyBoolean(), any(), any())).thenReturn(stats);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("users"))
                .andExpect(view().name("admin/user-list"));
    }
}
