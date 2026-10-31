package org.spring.createa.chessvalenti.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 공통 head(fragment/head.html :: common)가 실제로 렌더링되는지 확인한다.
 * Google Analytics 와 문자셋, 뷰포트 메타 태그가 페이지마다 한 번씩만 들어가야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommonHeadTemplateTest {

  private static final String GTAG_SRC = "googletagmanager.com/gtag/js?id=G-DBMLJY2F5L";

  @Autowired
  MockMvc mockMvc;

  @ParameterizedTest
  @ValueSource(strings = {"/login", "/signup", "/forgot-password", "/landing"})
  void publicPages_RenderTheCommonHeadOnce(String path) throws Exception {
    String html = mockMvc.perform(get(path))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(count(html, GTAG_SRC)).isEqualTo(1);
    assertThat(count(html, "gtag('config'")).isEqualTo(1);
    assertThat(count(html, "<meta charset=")).isEqualTo(1);
    assertThat(count(html, "name=\"viewport\"")).isEqualTo(1);
    assertThat(html).doesNotContain("th:replace");
  }

  private static int count(String text, String part) {
    int count = 0;
    for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) {
      count++;
    }
    return count;
  }
}
