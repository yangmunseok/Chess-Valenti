package org.spring.createa.chessvalenti.Repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest(showSql = false, properties = {
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.show_sql=false",
    "logging.level.org.hibernate.SQL=OFF",
    "logging.level.org.hibernate.orm.jdbc.bind=OFF"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories(basePackageClasses = GameIndexQueryTimingRepository.class)
@EntityScan(basePackageClasses = GameIndex.class)
public class GameIndexRepositoryTest {

  private static final Path TEST_DATASET = Path.of("data", "csv", "test_dataset.csv");
  private static final int WARMUP_ROUNDS = 3;
  private static final int MEASURE_ROUNDS = 10;

  @Autowired
  private GameIndexQueryTimingRepository gameIndexQueryTimingRepository;

  @Test
  @DisplayName("findAllByPawnStructureAndPieceConfiguration List query timing comparison")
  void compareFindAllByPawnStructureAndPieceConfigurationAverageTime() throws IOException {
    List<QueryCondition> conditions = readQueryConditions();

    assertThat(conditions).isNotEmpty();

    TimingResult defaultIndexResult = measure(
        "default index choice",
        conditions,
        gameIndexQueryTimingRepository::findAllByPawnStructureAndPieceConfiguration);

    TimingResult forcePawnIndexResult = measure(
        "force pawn_idx",
        conditions,
        gameIndexQueryTimingRepository::findAllByPawnStructureAndPieceConfigurationForcePawnIndex);

    assertThat(forcePawnIndexResult.totalRows()).isEqualTo(defaultIndexResult.totalRows());

    System.out.printf(
        "%nfindAllByPawnStructureAndPieceConfiguration(List) timing comparison%n"
            + "datasetRows=%d, warmupRounds=%d, measureRounds=%d, queriesPerCase=%d%n"
            + "%s%n"
            + "%s%n"
            + "forcePawnIndexAverageDeltaMs=%.3f, forcePawnIndexAverageRatio=%.3f%n",
        conditions.size(),
        WARMUP_ROUNDS,
        MEASURE_ROUNDS,
        defaultIndexResult.queryCount(),
        defaultIndexResult,
        forcePawnIndexResult,
        forcePawnIndexResult.averageMs() - defaultIndexResult.averageMs(),
        forcePawnIndexResult.averageMs() / defaultIndexResult.averageMs());
  }

  private static TimingResult measure(String label, List<QueryCondition> conditions,
      BiFunction<Long, Integer, List<GameIndex>> query) {
    for (int i = 0; i < WARMUP_ROUNDS; i++) {
      for (QueryCondition condition : conditions) {
        query.apply(condition.pawnStructure(), condition.pieceConfiguration());
      }
    }

    List<Long> elapsedNanos = new ArrayList<>(conditions.size() * MEASURE_ROUNDS);
    long totalRows = 0L;

    for (int i = 0; i < MEASURE_ROUNDS; i++) {
      for (QueryCondition condition : conditions) {
        long startedAt = System.nanoTime();
        List<GameIndex> result = query.apply(condition.pawnStructure(),
            condition.pieceConfiguration());
        long endedAt = System.nanoTime();

        elapsedNanos.add(endedAt - startedAt);
        totalRows += result.size();
      }
    }

    Collections.sort(elapsedNanos);

    int queryCount = elapsedNanos.size();
    double averageMs = elapsedNanos.stream()
        .mapToLong(Long::longValue)
        .average()
        .orElseThrow() / 1_000_000.0;

    return new TimingResult(
        label,
        queryCount,
        averageMs,
        toMillis(elapsedNanos.get(0)),
        toMillis(percentile(elapsedNanos, 0.50)),
        toMillis(percentile(elapsedNanos, 0.95)),
        toMillis(elapsedNanos.get(elapsedNanos.size() - 1)),
        totalRows,
        totalRows / (double) queryCount);
  }

  private static List<QueryCondition> readQueryConditions() throws IOException {
    return Files.readAllLines(TEST_DATASET).stream()
        .skip(1)
        .filter(line -> !line.isBlank())
        .map(GameIndexRepositoryTest::parseQueryCondition)
        .toList();
  }

  private static QueryCondition parseQueryCondition(String line) {
    String[] values = line.split(",");

    if (values.length != 2) {
      throw new IllegalArgumentException("Invalid test dataset row: " + line);
    }

    return new QueryCondition(Long.parseLong(values[0].trim()),
        Integer.parseInt(values[1].trim()));
  }

  private static long percentile(List<Long> sortedValues, double percentile) {
    int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
    return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
  }

  private static double toMillis(long nanos) {
    return nanos / 1_000_000.0;
  }

  private record QueryCondition(long pawnStructure, int pieceConfiguration) {
  }

  private record TimingResult(
      String label,
      int queryCount,
      double averageMs,
      double minMs,
      double p50Ms,
      double p95Ms,
      double maxMs,
      long totalRows,
      double averageRows) {

    @Override
    public String toString() {
      return String.format(
          "%s: averageMs=%.3f, minMs=%.3f, p50Ms=%.3f, p95Ms=%.3f, maxMs=%.3f, "
              + "totalRowsReturned=%d, averageRowsReturned=%.3f",
          label, averageMs, minMs, p50Ms, p95Ms, maxMs, totalRows, averageRows);
    }
  }
}
