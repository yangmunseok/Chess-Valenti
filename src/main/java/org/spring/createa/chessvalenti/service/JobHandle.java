package org.spring.createa.chessvalenti.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * 분석 job 하나의 취소 상태를 관리한다.
 *
 * <p>외부 API를 읽는 구간(beginFetch ~ endFetch)에서만 스레드를 인터럽트하고 스트림을 닫는다.
 * 그 밖의 구간(필터링, DB 저장)은 취소되어도 끝까지 실행되어 부분 결과가 저장된다.
 */
public class JobHandle {

  private volatile boolean cancelled;
  private volatile Future<?> future;
  private Thread fetchThread;
  private Closeable stream;

  public boolean isCancelled() {
    return cancelled;
  }

  void setFuture(Future<?> future) {
    this.future = future;
  }

  Future<?> getFuture() {
    return future;
  }

  public synchronized void cancel() {
    cancelled = true;
    if (fetchThread != null) {
      closeQuietly(stream);
      fetchThread.interrupt();
    }
  }

  public synchronized void beginFetch() {
    fetchThread = Thread.currentThread();
  }

  public synchronized void endFetch() {
    fetchThread = null;
    stream = null;
    // 읽기 구간에서 받은 인터럽트가 DB 저장 같은 이후 작업을 깨뜨리지 않도록 지움
    Thread.interrupted();
  }

  /**
   * 지금 읽고 있는 응답 스트림을 등록한다. 취소되면 이 스트림을 닫아 블로킹된 읽기를 깨운다.
   */
  public synchronized void attachStream(Closeable stream) {
    this.stream = stream;
    if (cancelled) {
      closeQuietly(stream);
    }
  }

  private static void closeQuietly(Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException ignored) {
      // 취소 중이므로 닫기 실패는 무시
    }
  }
}
