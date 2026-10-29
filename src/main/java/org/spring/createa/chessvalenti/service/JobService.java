package org.spring.createa.chessvalenti.service;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JobService {

  // 동시에 실행되는 분석 수. 나머지는 대기열에서 순서대로 기다린다.
  private static final int MAX_RUNNING = 3;
  private static final int MAX_WAITING = 50;
  // 외부 API가 응답 없이 멈춰도 작업 스레드가 영원히 묶이지 않도록 하는 상한
  private static final Duration JOB_TIMEOUT = Duration.ofMinutes(10);

  private final Map<Long, JobHandle> jobs = new ConcurrentHashMap<>();
  private final AtomicLong cnt = new AtomicLong(0);
  private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
      MAX_RUNNING, MAX_RUNNING, 0L, TimeUnit.MILLISECONDS,
      new ArrayBlockingQueue<>(MAX_WAITING),
      namedThreadFactory("insight-"),
      new ThreadPoolExecutor.AbortPolicy());
  private final ScheduledExecutorService watchdog =
      Executors.newSingleThreadScheduledExecutor(namedThreadFactory("insight-watchdog-"));

  public void work(long id, Consumer<JobHandle> task) {
    work(id, null, task);
  }

  public void work(long id, String owner, Consumer<JobHandle> task) {
    JobHandle handle = new JobHandle(owner);
    if (jobs.putIfAbsent(id, handle) != null) {
      throw new RuntimeException("invalid job id");
    }

    try {
      handle.setFuture(executor.submit(() -> run(id, handle, task)));
    } catch (RejectedExecutionException e) {
      jobs.remove(id, handle);
      log.warn("Job {} rejected (running: {}, waiting: {})", id, executor.getActiveCount(),
          executor.getQueue().size());
      throw new RuntimeException("현재 분석 요청이 많습니다. 잠시 후 다시 시도해주세요.");
    }
    log.info("Job {} queued (running: {}, waiting: {})", id, executor.getActiveCount(),
        executor.getQueue().size());
  }

  private void run(long id, JobHandle handle, Consumer<JobHandle> task) {
    log.info("Job {} started", id);
    ScheduledFuture<?> timeout = watchdog.schedule(() -> {
      log.warn("Job {} timed out after {}", id, JOB_TIMEOUT);
      handle.cancel();
    }, JOB_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

    try {
      task.accept(handle);
    } catch (RuntimeException e) {
      log.error("Job {} failed", id, e);
    } finally {
      timeout.cancel(false);
      jobs.remove(id, handle);
      log.info("Job {} finished (cancelled: {})", id, handle.isCancelled());
    }
  }

  public void dispose(long id) {
    dispose(id, null);
  }

  public void dispose(long id, String requester) {
    JobHandle handle = jobs.get(id);
    if (handle == null) {
      return;
    }
    if (requester != null && !handle.isOwnedBy(requester)) {
      log.warn("Session {} tried to cancel job {} owned by another session", requester, id);
      return;
    }
    cancel(id, handle, false);
  }

  // 연결이 끊긴 세션의 job은 결과를 받을 사람이 없으므로 모두 취소하고, 부분 결과도 저장하지 않음
  public void disposeAllOf(String owner) {
    jobs.forEach((id, handle) -> {
      if (owner != null && owner.equals(handle.getOwner())) {
        cancel(id, handle, true);
      }
    });
  }

  private void cancel(long id, JobHandle handle, boolean discardResult) {
    if (discardResult) {
      handle.cancelAndDiscard();
    } else {
      handle.cancel();
    }
    // 아직 대기열에 있던 job은 실행되지 않으므로 여기서 정리
    Future<?> future = handle.getFuture();
    if (future != null && executor.remove((Runnable) future)) {
      jobs.remove(id, handle);
      log.info("Job {} removed from queue", id);
    }
  }

  public long getAvailableId() {
    return cnt.getAndIncrement();
  }

  @PreDestroy
  void shutdown() {
    // 서버 종료 중에 부분 결과로 기존 분석을 덮어쓰지 않도록 버림
    jobs.values().forEach(JobHandle::cancelAndDiscard);
    executor.shutdownNow();
    watchdog.shutdownNow();
  }

  private static ThreadFactory namedThreadFactory(String prefix) {
    AtomicInteger seq = new AtomicInteger(1);
    return runnable -> {
      Thread thread = new Thread(runnable, prefix + seq.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    };
  }
}
