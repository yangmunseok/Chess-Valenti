package org.spring.createa.chessvalenti.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class JobService {

  private final Map<Long, Disposable> jobs = new ConcurrentHashMap<>();
  private final AtomicLong cnt = new AtomicLong(0);
  private final Semaphore semaphore = new Semaphore(3);

  public void work(Mono<Object> job, long id) {
    if (jobs.containsKey(id)) {
      throw new RuntimeException("invalid job id");
    }

    if (!semaphore.tryAcquire()) {
      throw new RuntimeException("현재 서버의 분석 자원이 부족합니다. 잠시 후 다시 시도해주세요.");
    }

    Disposable disposable = job
        .doFinally(signalType -> {
          semaphore.release();
          jobs.remove(id);
          log.info("Job {} finished with signal {}", id, signalType);
        })
        .subscribe(
            result -> log.debug("Job {} completed successfully", id),
            error -> log.error("Job {} failed", id, error)
        );

    jobs.put(id, disposable);
  }

  public void dispose(long id) {
    Disposable disposable = jobs.get(id);
    if (disposable != null) {
      disposable.dispose();
    }
  }

  public long getAvailableId() {
    return cnt.getAndIncrement();
  }
}
