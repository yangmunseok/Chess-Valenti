package org.spring.createa.chessvalenti.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * 분석 job 하나의 취소 상태를 관리한다.
 *
 * <p>외부 API를 읽는 구간({@link #fetch(Runnable)})에서만 스레드를 인터럽트하고 스트림을 닫는다.
 * 그 밖의 구간(필터링, DB 저장)은 취소되어도 끝까지 실행되어 부분 결과가 저장된다.
 */
public class JobHandle implements CancellationToken {

  // job을 요청한 STOMP 세션. 이 세션만 job을 취소할 수 있다.
  private final String owner;
  private volatile boolean cancelled;
  // true면 취소 후 부분 결과를 저장하지도 보내지도 않는다 (연결 끊김, 서버 종료)
  private volatile boolean discardResult;
  private volatile Future<?> future;
  private Thread fetchThread;
  private Closeable stream;

  public JobHandle() {
    this(null);
  }

  public JobHandle(String owner) {
    this.owner = owner;
  }

  String getOwner() {
    return owner;
  }

  boolean isOwnedBy(String sessionId) {
    return owner == null || owner.equals(sessionId);
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  void setFuture(Future<?> future) {
    this.future = future;
  }

  Future<?> getFuture() {
    return future;
  }

  public boolean isResultDiscarded() {
    return discardResult;
  }

  public void cancelAndDiscard() {
    discardResult = true;
    cancel();
  }

  public synchronized void cancel() {
    cancelled = true;
    if (fetchThread != null) {
      closeQuietly(stream);
      fetchThread.interrupt();
    }
  }

  /**
   * 외부 API를 읽는 작업을 실행한다. 실행 중에 취소되면 스트림을 닫고 스레드를 인터럽트한다.
   * 끝나면 인터럽트 상태를 지워 이후의 DB 저장 같은 작업이 영향을 받지 않게 한다.
   */
  public void fetch(Runnable action) {
    beginFetch();
    try {
      action.run();
    } finally {
      endFetch();
    }
  }

  private synchronized void beginFetch() {
    fetchThread = Thread.currentThread();
  }

  private synchronized void endFetch() {
    fetchThread = null;
    stream = null;
    Thread.interrupted();
  }

  @Override
  public synchronized void onCancel(Closeable stream) {
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
