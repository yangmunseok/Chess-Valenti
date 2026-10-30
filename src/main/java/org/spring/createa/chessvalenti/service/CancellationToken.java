package org.spring.createa.chessvalenti.service;

import java.io.Closeable;

/**
 * 외부 API를 읽는 코드에 넘기는 취소 신호.
 *
 * <p>읽는 쪽은 job이 무엇인지 몰라도 되고, 이 두 가지만 하면 된다.
 * 반복할 때마다 {@link #isCancelled()}를 확인하고, 응답 스트림을 열면 {@link #onCancel(Closeable)}로
 * 등록한다. 취소되면 등록된 스트림이 닫혀 블로킹된 읽기가 깨어난다.
 */
public interface CancellationToken {

  boolean isCancelled();

  /**
   * 취소되면 닫을 스트림을 등록한다. 이미 취소된 상태면 바로 닫는다.
   */
  void onCancel(Closeable stream);
}
