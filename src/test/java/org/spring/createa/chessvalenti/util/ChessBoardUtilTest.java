package org.spring.createa.chessvalenti.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ChessBoardUtil.class})
public class ChessBoardUtilTest {

  ChessBoardUtil chessBoardUtil;

  public ChessBoardUtilTest(ChessBoardUtil chessBoardUtil) {
    this.chessBoardUtil = chessBoardUtil;
  }

  public void testCountDoubled() {

  }

  public void testCountIsolated() {

  }

  public void testIsSimilar() {

  }

  public void testPawnOnlyFen() {

  }

  public void testMeterialScore() {
    
  }
}
