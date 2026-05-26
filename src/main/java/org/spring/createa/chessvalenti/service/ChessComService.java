package org.spring.createa.chessvalenti.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChessComService {

  private static final Pattern ARCHIVE_DATE_PATTERN = Pattern.compile("/games/(\\d{4})/(\\d{2})$");
  private static final Pattern PGN_HEADER_PATTERN = Pattern.compile("^\\[(\\w+)\\s+\"(.*)\"\\]$");

  private final ChessComApi chessComApi;

  public Flux<InsightGame> loadGames(String username, String perfType, String since) {
    String normalizedUsername = username.toLowerCase(Locale.ROOT);
    log.info("Loading Chess.com games for {}", normalizedUsername);
    long sinceMillis = parseSince(since);
    YearMonth sinceMonth = Instant.ofEpochMilli(sinceMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .withDayOfMonth(1)
        .query(YearMonth::from);
    Set<String> requestedTimeClasses = parsePerfTypes(perfType);

    return chessComApi.loadArchives(normalizedUsername)
        .flatMapMany(response -> Flux.fromIterable(
            response.archives() == null ? List.of() : response.archives()))
        .map(this::toArchiveMonth)
        .filter(archive -> !archive.yearMonth().isBefore(sinceMonth))
        .concatMap(archive -> chessComApi.loadMonthlyPgn(normalizedUsername, archive.year(),
            archive.month()))
        .flatMapIterable(this::parsePgnGames)
        .filter(game -> isSupportedGame(game, requestedTimeClasses, sinceMillis));
  }

  private boolean isSupportedGame(InsightGame game, Set<String> requestedTimeClasses,
      long sinceMillis) {
    Map<String, String> headers = parseHeaders(game.pgn());
    return "Chess".equalsIgnoreCase(headers.getOrDefault("Variant", "Chess"))
        && requestedTimeClasses.contains(toTimeClass(headers))
        && getGameDateMillis(headers) >= sinceMillis;
  }

  private List<InsightGame> parsePgnGames(String monthlyPgn) {
    log.debug("parsePgnGames invoked");
    if (monthlyPgn == null || monthlyPgn.isBlank()) {
      return List.of();
    }

    String normalized = monthlyPgn.replace("\r\n", "\n").trim();
    String[] rawGames = normalized.split("(?m)(?=^\\[Event\\s+\")");
    List<InsightGame> games = new ArrayList<>();

    for (String rawGame : rawGames) {
      String pgn = rawGame.trim();
      if (pgn.isEmpty()) {
        continue;
      }
      Map<String, String> headers = parseHeaders(pgn);
      games.add(new InsightGame(getWinner(headers), pgn, headers.get("White"), headers.get("Black"),
          headers.getOrDefault("Variant", "Chess").toLowerCase(Locale.ROOT)));
    }

    return games;
  }

  private Map<String, String> parseHeaders(String pgn) {
    Map<String, String> headers = new LinkedHashMap<>();
    for (String line : pgn.split("\\R")) {
      if (!line.startsWith("[")) {
        break;
      }
      Matcher matcher = PGN_HEADER_PATTERN.matcher(line);
      if (matcher.matches()) {
        headers.put(matcher.group(1), matcher.group(2));
      }
    }
    return headers;
  }

  private String getWinner(Map<String, String> headers) {
    String result = headers.getOrDefault("Result", "");
    if ("1-0".equals(result)) {
      return "white";
    }
    if ("0-1".equals(result)) {
      return "black";
    }
    return "";
  }

  private String toTimeClass(Map<String, String> headers) {
    String timeClass = headers.get("TimeClass");
    if (timeClass != null && !timeClass.isBlank()) {
      return timeClass.toLowerCase(Locale.ROOT);
    }

    String timeControl = headers.getOrDefault("TimeControl", "");
    int baseSeconds = parseBaseSeconds(timeControl);
    if (baseSeconds < 180) {
      return "bullet";
    }
    if (baseSeconds < 600) {
      return "blitz";
    }
    return "rapid";
  }

  private int parseBaseSeconds(String timeControl) {
    if (timeControl == null || timeControl.isBlank() || "-".equals(timeControl)) {
      return 0;
    }
    String base = timeControl.split("\\+")[0];
    try {
      return Integer.parseInt(base);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private long getGameDateMillis(Map<String, String> headers) {
    String date = headers.get("UTCDate");
    if (date == null || date.isBlank()) {
      date = headers.get("Date");
    }
    if (date == null || date.contains("?")) {
      return 0L;
    }
    return LocalDate.parse(date.replace('.', '-'))
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli();
  }

  private ArchiveMonth toArchiveMonth(String archiveUrl) {
    log.info("toArchiveMonth invoked");
    Matcher matcher = ARCHIVE_DATE_PATTERN.matcher(archiveUrl);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Unexpected Chess.com archive URL: " + archiveUrl);
    }

    String year = matcher.group(1);
    String month = matcher.group(2);
    return new ArchiveMonth(year, month,
        YearMonth.of(Integer.parseInt(year), Integer.parseInt(month)));
  }

  private Set<String> parsePerfTypes(String perfType) {
    if (perfType == null || perfType.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(perfType.split(","))
        .map(value -> value.trim().toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  private long parseSince(String since) {
    try {
      return Long.parseLong(since);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid since value: " + since, e);
    }
  }

  private record ArchiveMonth(String year, String month, YearMonth yearMonth) {

  }
}
