package org.spring.createa.chessvalenti.util;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ChessHashHelper.class})
public class ChessHashHelperTest {

  ChessHashHelper chessHashHelper;

  @Autowired
  public ChessHashHelperTest(ChessHashHelper chessHashHelper) {
    this.chessHashHelper = chessHashHelper;
  }


  @Test
  void checkColision() {
    // it will soon added.
  }

  @Test
  void testGetActiveBits() {
    /*
     * getActiveBits 설명서
     *
     * 입력:
     *   - 정수 값 (long)
     *
     * 처리:
     *   1. 입력 값의 이진 표현에서 각 비트를 검사한다.
     *   2. 값이 1로 설정된 비트(활성 비트)의 위치를 식별한다.
     *
     * 출력:
     *   - 활성 비트들의 위치를 담은 리스트 반환
     *     (일반적으로 LSB를 0번 인덱스로 사용)
     *
     * 예:
     *   입력:  0b10110
     *   출력:  [1, 2, 4]
     */

    //2 = 0b10
    Assertions.assertEquals(List.of(), chessHashHelper.getActiveBits(2));
    //1024 = 0b10000000000
    Assertions.assertEquals(chessHashHelper.getActiveBits(1024), List.of(10));
    //1536 = 0b11000000000
    Assertions.assertEquals(chessHashHelper.getActiveBits(1536), List.of(9, 10));

  }

  @Test
  void testGenerateInputs() {
    /*
     * generateInput 설명서 1
     *
     * 입력:
     *   - 정수 리스트
     *
     * 처리:
     *   1. 리스트의 모든 원소에 대해 8을 뺀다.
     *   2. 리스트의 길이가 8 미만인 경우,
     *      값 48부터 시작하여 1씩 증가시키며
     *      리스트의 길이가 8이 될 때까지 원소를 추가한다.
     *
     * 출력:
     *   - 최종 길이가 8인 리스트 반환
     */
    Assertions.assertEquals(chessHashHelper.generateInputs(List.of(0)),
        List.of(-8, 48, 49, 50, 51, 52, 53, 54));
    Assertions.assertEquals(chessHashHelper.generateInputs(List.of(9)),
        List.of(1, 48, 49, 50, 51, 52, 53, 54));
    Assertions.assertEquals(chessHashHelper.generateInputs(List.of(10)),
        List.of(2, 48, 49, 50, 51, 52, 53, 54));

    /*
     * generateInput 설명서 2
     *
     * 입력:
     *   - 정수 값 (long)
     *
     * 처리:
     *   1. 입력 값에서 활성 비트들의 위치 리스트를 추출한다.
     *      (getActiveBits 사용)
     *   2. 해당 리스트를 기반으로 길이가 8인 리스트로 가공한다.
     *      (generateInput(List) 사용)
     *
     * 출력:
     *   - 길이가 8인 정수 리스트 반환
     */
    Assertions.assertEquals(chessHashHelper.generateInputs(0),
        List.of(48, 49, 50, 51, 52, 53, 54, 55));
    Assertions.assertEquals(chessHashHelper.generateInputs(512),
        List.of(1, 48, 49, 50, 51, 52, 53, 54));
    Assertions.assertEquals(chessHashHelper.generateInputs(1024),
        List.of(2, 48, 49, 50, 51, 52, 53, 54));
  }

  /*
  @Test
  void libTest() {
    Board board = new Board();
    board.loadFromFen("8/p1p3pp/5p2/P1p1p3/4P3/3P3P/1PP3P1/8 w");
  }
   */
}
