package org.spring.createa.chessvalenti.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Service
public class JobService {

  Map<Long, Disposable> jobs;
  long cnt;

  public JobService() {
    cnt = 0;
    jobs = new HashMap<>();
  }

  public void work(Mono<Object> job, long id) {
    if (jobs.containsKey(id)) {
      throw new RuntimeException("invalid job id");
    }
    jobs.put(id, job.subscribe());
    job.doOnSuccess((result) -> {
      jobs.remove(id);
    });
    cnt++;
  }

  public void dispose(long id) {
    Disposable disposable = jobs.get(id);
    disposable.dispose();
    jobs.remove(id);
  }

  public long getAvailableId() {
    return cnt;
  }
}
