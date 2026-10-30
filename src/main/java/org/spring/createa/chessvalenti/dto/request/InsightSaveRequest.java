package org.spring.createa.chessvalenti.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.spring.createa.chessvalenti.dto.game.GameResults;

/**
 * 브라우저에서 계산한 인사이트 결과. 브라우저가 보낸 값은 조작될 수 있으므로 크기와 형식을 검증한다.
 */
public record InsightSaveRequest(
    @Schema(description = "분석한 Lichess 또는 Chess.com 아이디")
    @NotBlank @Size(max = 50) String username,

    @Schema(description = "게임 유형 (쉼표로 구분)", example = "blitz,rapid")
    @NotBlank @Pattern(regexp = PERF_TYPES) String perfType,

    @Schema(description = "분석 시작 시각 (epoch 밀리초)")
    @NotNull @PositiveOrZero Long since,

    @Schema(description = "폰 구조(폰만 남긴 FEN)별 승패. 화면에 보여 주는 상위 구조만 담는다.")
    @NotNull @Size(max = MAX_STRUCTURES) Map<String, GameResults> data) {

  // 화면의 세 목록(많이 둔 구조 5, 승률 5, 패배율 6)을 합친 최대 개수
  public static final int MAX_STRUCTURES = 16;

  private static final String PERF_TYPE = "(bullet|blitz|rapid|classical)";
  private static final String PERF_TYPES = PERF_TYPE + "(," + PERF_TYPE + ")*";
}
