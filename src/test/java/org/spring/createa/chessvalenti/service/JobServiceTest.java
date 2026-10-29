package org.spring.createa.chessvalenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JobServiceTest {

  private final JobService jobService = new JobService();
  private final CountDownLatch release = new CountDownLatch(1);

  @AfterEach
  void tearDown() {
    release.countDown();
    jobService.shutdown();
  }

  private long submitBlocking(CountDownLatch started) {
    long id = jobService.getAvailableId();
    jobService.work(id, handle -> {
      started.countDown();
      await(release);
    });
    return id;
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void work_ShouldRunAtMostThreeJobsAtOnce() throws InterruptedException {
    AtomicInteger running = new AtomicInteger();
    AtomicInteger maxRunning = new AtomicInteger();
    CountDownLatch finished = new CountDownLatch(6);

    for (int i = 0; i < 6; i++) {
      jobService.work(jobService.getAvailableId(), handle -> {
        maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
        await(release);
        running.decrementAndGet();
        finished.countDown();
      });
    }

    Thread.sleep(200);
    assertThat(running.get()).isEqualTo(3);
    release.countDown();
    assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(maxRunning.get()).isEqualTo(3);
  }

  @Test
  void work_ShouldRunWaitingJobsInOrder() throws InterruptedException {
    CountDownLatch started = new CountDownLatch(1);
    List<Integer> order = new CopyOnWriteArrayList<>();
    CountDownLatch done = new CountDownLatch(3);

    // 실행 스레드 하나만 비워 두고 나머지 둘은 계속 막아 둠
    jobService.work(jobService.getAvailableId(), handle -> started.countDown());
    submitBlocking(new CountDownLatch(1));
    submitBlocking(new CountDownLatch(1));
    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

    CountDownLatch gate = new CountDownLatch(1);
    jobService.work(jobService.getAvailableId(), handle -> await(gate));
    for (int i = 1; i <= 3; i++) {
      int n = i;
      jobService.work(jobService.getAvailableId(), handle -> {
        order.add(n);
        done.countDown();
      });
    }

    gate.countDown();
    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(order).containsExactly(1, 2, 3);
  }

  @Test
  void work_WhenQueueIsFull_ShouldReject() {
    for (int i = 0; i < 3 + 50; i++) {
      submitBlocking(new CountDownLatch(1));
    }

    assertThatThrownBy(() -> jobService.work(jobService.getAvailableId(), handle -> {
    })).isInstanceOf(RuntimeException.class).hasMessageContaining("요청이 많습니다");
  }

  @Test
  void dispose_WhenJobIsWaiting_ShouldRemoveItFromQueue() throws InterruptedException {
    for (int i = 0; i < 3; i++) {
      submitBlocking(new CountDownLatch(1));
    }
    AtomicBoolean ran = new AtomicBoolean(false);
    long waiting = jobService.getAvailableId();
    jobService.work(waiting, handle -> ran.set(true));

    jobService.dispose(waiting);
    release.countDown();
    Thread.sleep(200);

    assertThat(ran).isFalse();
  }

  @Test
  void dispose_WhenJobIsRunning_ShouldCancelHandle() throws InterruptedException {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch observed = new CountDownLatch(1);
    long id = jobService.getAvailableId();
    jobService.work(id, handle -> {
      started.countDown();
      while (!handle.isCancelled()) {
        Thread.onSpinWait();
      }
      observed.countDown();
    });
    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

    jobService.dispose(id);

    assertThat(observed.await(5, TimeUnit.SECONDS)).isTrue();
  }

  private long submitCancellable(String owner, CountDownLatch cancelled) {
    CountDownLatch started = new CountDownLatch(1);
    long id = jobService.getAvailableId();
    jobService.work(id, owner, handle -> {
      started.countDown();
      long deadline = System.currentTimeMillis() + 1_000;
      while (!handle.isCancelled() && System.currentTimeMillis() < deadline) {
        Thread.onSpinWait();
      }
      if (handle.isCancelled()) {
        cancelled.countDown();
      }
    });
    await(started);
    return id;
  }

  @Test
  void dispose_FromAnotherSession_ShouldBeIgnored() throws InterruptedException {
    CountDownLatch cancelled = new CountDownLatch(1);
    long id = submitCancellable("owner", cancelled);

    jobService.dispose(id, "intruder");

    assertThat(cancelled.await(1_500, TimeUnit.MILLISECONDS)).isFalse();
  }

  @Test
  void disposeAllOf_ShouldCancelOnlyThatSessionsJobs() throws InterruptedException {
    CountDownLatch mineCancelled = new CountDownLatch(2);
    CountDownLatch otherCancelled = new CountDownLatch(1);
    submitCancellable("me", mineCancelled);
    submitCancellable("me", mineCancelled);
    submitCancellable("other", otherCancelled);

    jobService.disposeAllOf("me");

    assertThat(mineCancelled.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(otherCancelled.await(1_500, TimeUnit.MILLISECONDS)).isFalse();
  }

  @Test
  void disposeAllOf_ShouldDiscardResult_ButDisposeShouldKeepIt() throws InterruptedException {
    CountDownLatch started = new CountDownLatch(2);
    CountDownLatch finished = new CountDownLatch(2);
    AtomicBoolean cancelledDiscarded = new AtomicBoolean(true);
    AtomicBoolean disconnectedDiscarded = new AtomicBoolean(false);

    long cancelled = jobService.getAvailableId();
    jobService.work(cancelled, "a", handle -> {
      started.countDown();
      while (!handle.isCancelled()) {
        Thread.onSpinWait();
      }
      cancelledDiscarded.set(handle.isResultDiscarded());
      finished.countDown();
    });
    jobService.work(jobService.getAvailableId(), "b", handle -> {
      started.countDown();
      while (!handle.isCancelled()) {
        Thread.onSpinWait();
      }
      disconnectedDiscarded.set(handle.isResultDiscarded());
      finished.countDown();
    });
    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

    jobService.dispose(cancelled, "a");
    jobService.disposeAllOf("b");

    assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(cancelledDiscarded).isFalse();
    assertThat(disconnectedDiscarded).isTrue();
  }

  @Test
  void work_WhenTaskThrows_ShouldKeepRunningOtherJobs() throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      jobService.work(jobService.getAvailableId(), handle -> {
        throw new IllegalStateException("boom");
      });
    }
    CountDownLatch ran = new CountDownLatch(1);
    jobService.work(jobService.getAvailableId(), handle -> ran.countDown());

    assertThat(ran.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void work_WithDuplicateId_ShouldReject() {
    long id = submitBlocking(new CountDownLatch(1));

    assertThatThrownBy(() -> jobService.work(id, handle -> {
    })).hasMessageContaining("invalid job id");
  }
}
