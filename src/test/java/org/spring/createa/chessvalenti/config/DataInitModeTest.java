package org.spring.createa.chessvalenti.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.spring.createa.chessvalenti.config.DataInitializer.DataInitMode;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class DataInitModeTest {

  @Test
  void from_ReadsModesCaseInsensitively() {
    assertThat(DataInitMode.from("OFF")).isEqualTo(DataInitMode.OFF);
    assertThat(DataInitMode.from(" pgn_to_db ")).isEqualTo(DataInitMode.PGN_TO_DB);
  }

  // 따옴표 없는 OFF 는 YAML 에서 불리언 false 로 읽혀 "false" 가 들어온다
  @ParameterizedTest
  @ValueSource(strings = {"false", "", "PGN"})
  void from_WithUnknownValue_ShouldStopInsteadOfImporting(String value) {
    assertThatThrownBy(() -> DataInitMode.from(value))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("chess.data-init.mode");
  }

  @Test
  void from_WithoutValue_ShouldStop() {
    assertThatThrownBy(() -> DataInitMode.from(null))
        .isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"application-dev.yml", "application-prod.yml"})
  void profileYaml_ShouldTurnDataInitOff(String file) throws Exception {
    List<PropertySource<?>> sources =
        new YamlPropertySourceLoader().load(file, new ClassPathResource(file));

    Object mode = sources.get(0).getProperty("chess.data-init.mode");

    assertThat(DataInitMode.from(String.valueOf(mode))).isEqualTo(DataInitMode.OFF);
  }
}
