package org.spring.createa.chessvalenti.service;

import com.github.bhlangonijr.chesslib.Board;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.spring.createa.chessvalenti.dto.response.StockfishEvaluationResponse;
import org.spring.createa.chessvalenti.dto.response.StockfishEvaluationResponse.PrincipalVariation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockfishService {

  private static final Pattern SCORE_PATTERN =
      Pattern.compile("\\bscore\\s+(cp|mate)\\s+(-?\\d+)\\b");

  @Value("${stockfish.path:stockfish}")
  private String stockfishPath;

  @Value("${stockfish.depth:12}")
  private int depth;

  @Value("${stockfish.timeout-seconds:8}")
  private long timeoutSeconds;

  public StockfishEvaluationResponse evaluate(String fen) {
    validateFen(fen);

    Process process = null;
    try {
      process = new ProcessBuilder(stockfishPath).redirectErrorStream(true).start();

      try (BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

        send(writer, "uci");
        waitFor(reader, "uciok", Duration.ofSeconds(timeoutSeconds));
        send(writer, "isready");
        waitFor(reader, "readyok", Duration.ofSeconds(timeoutSeconds));
        send(writer, "ucinewgame");
        send(writer, "position fen " + fen);
        send(writer, "go depth " + depth);

        PrincipalVariation evaluation = toWhitePerspective(
            readEvaluation(reader, Duration.ofSeconds(timeoutSeconds)), fen);
        send(writer, "quit");
        return new StockfishEvaluationResponse(List.of(evaluation));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Stockfish executable not found or failed to start: "
          + stockfishPath, e);
    } finally {
      if (process != null && process.isAlive()) {
        process.destroy();
        try {
          if (!process.waitFor(1, TimeUnit.SECONDS)) {
            process.destroyForcibly();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          process.destroyForcibly();
        }
      }
    }
  }

  public void streamEvaluation(String fen, Consumer<StockfishEvaluationResponse> evaluationConsumer,
      BooleanSupplier shouldContinue) {
    validateFen(fen);

    Process process = null;
    try {
      process = new ProcessBuilder(stockfishPath).redirectErrorStream(true).start();

      try (BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

        send(writer, "uci");
        waitFor(reader, "uciok", Duration.ofSeconds(timeoutSeconds));
        send(writer, "isready");
        waitFor(reader, "readyok", Duration.ofSeconds(timeoutSeconds));
        send(writer, "ucinewgame");
        send(writer, "position fen " + fen);
        send(writer, "go depth " + depth);

        readEvaluations(reader, fen, evaluationConsumer, shouldContinue);
        send(writer, "quit");
      }
    } catch (IOException e) {
      throw new IllegalStateException("Stockfish executable not found or failed to start: "
          + stockfishPath, e);
    } finally {
      if (process != null && process.isAlive()) {
        process.destroy();
        try {
          if (!process.waitFor(1, TimeUnit.SECONDS)) {
            process.destroyForcibly();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          process.destroyForcibly();
        }
      }
    }
  }

  private void validateFen(String fen) {
    if (fen == null || fen.isBlank()) {
      throw new IllegalArgumentException("fen is required");
    }

    Board board = new Board();
    board.loadFromFen(fen);
  }

  private void send(BufferedWriter writer, String command) throws IOException {
    writer.write(command);
    writer.newLine();
    writer.flush();
  }

  private void waitFor(BufferedReader reader, String expected, Duration timeout) throws IOException {
    long deadline = System.nanoTime() + timeout.toNanos();
    String line;
    while (System.nanoTime() < deadline && (line = reader.readLine()) != null) {
      if (expected.equals(line.trim())) {
        return;
      }
    }
    throw new IllegalStateException("Timed out waiting for Stockfish response: " + expected);
  }

  private PrincipalVariation readEvaluation(BufferedReader reader, Duration timeout) throws IOException {
    long deadline = System.nanoTime() + timeout.toNanos();
    PrincipalVariation latestScore = null;
    String line;

    while (System.nanoTime() < deadline && (line = reader.readLine()) != null) {
      Matcher matcher = SCORE_PATTERN.matcher(line);
      if (matcher.find()) {
        int score = Integer.parseInt(matcher.group(2));
        latestScore = "mate".equals(matcher.group(1))
            ? new PrincipalVariation(null, score)
            : new PrincipalVariation(score, null);
      }

      if (line.startsWith("bestmove")) {
        if (latestScore == null) {
          throw new IllegalStateException("Stockfish did not return an evaluation score");
        }
        return latestScore;
      }
    }

    throw new IllegalStateException("Timed out waiting for Stockfish evaluation");
  }

  private void readEvaluations(BufferedReader reader, String fen,
      Consumer<StockfishEvaluationResponse> evaluationConsumer, BooleanSupplier shouldContinue)
      throws IOException {
    String line;
    while (shouldContinue.getAsBoolean() && (line = reader.readLine()) != null) {
      Matcher matcher = SCORE_PATTERN.matcher(line);
      if (matcher.find()) {
        int score = Integer.parseInt(matcher.group(2));
        PrincipalVariation evaluation = "mate".equals(matcher.group(1))
            ? new PrincipalVariation(null, score)
            : new PrincipalVariation(score, null);
        evaluationConsumer.accept(new StockfishEvaluationResponse(
            List.of(toWhitePerspective(evaluation, fen))));
      }

      if (line.startsWith("bestmove")) {
        return;
      }
    }
  }

  private PrincipalVariation toWhitePerspective(PrincipalVariation evaluation, String fen) {
    boolean blackToMove = fen.split("\\s+")[1].equals("b");
    if (!blackToMove) {
      return evaluation;
    }

    if (evaluation.cp() != null) {
      return new PrincipalVariation(-evaluation.cp(), null);
    }
    return new PrincipalVariation(null, -evaluation.mate());
  }
}
