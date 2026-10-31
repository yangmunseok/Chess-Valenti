package org.spring.createa.chessvalenti.config;

import com.github.bhlangonijr.chesslib.game.Player;
import com.github.bhlangonijr.chesslib.move.Move;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.ChessPlayerRepository;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.CustomGame;
import org.spring.createa.chessvalenti.service.GameService;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.spring.createa.chessvalenti.util.GameProcessor;
import org.spring.createa.chessvalenti.util.pgn.CustomPgnIterator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

  private static final String DEFAULT_STARTING_FEN =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  private final GameIndexRepository gameIndexRepository;
  private final ChessHashHelper chessHashHelper;
  private final ChessPlayerRepository chessPlayerRepository;
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final GameService gameService;
  private final ObjectProvider<GameProcessor> gameProcessorObjectProvider;

  Map<String, ChessPlayer> playerCache;

  @Value("${chess.pgn.path}")
  private String pgnPath;

  @Value("${admin.username}")
  private String adminUsername;

  @Value("${admin.password}")
  private String adminPassword;

  @Value("${admin.email}")
  private String adminEmail;

  @Value("${chess.data-init.mode:OFF}")
  private String initMode;

  @Value("${chess.data-init.csv-dir:data/csv}")
  private String csvDir;

  public enum DataInitMode {
    PGN_TO_DB,
    PGN_TO_CSV,
    CSV_TO_DB,
    OFF;

    /**
     * 설정 값을 모드로 바꾼다. 모르는 값이면 기동을 멈춘다.
     *
     * <p>예전에는 모르는 값을 PGN_TO_DB 로 바꿨다. YAML 에서 따옴표 없는 OFF 는 불리언 false 로 읽혀 "false"
     * 가 들어오는데, 그 때문에 끄려던 대량 적재가 오히려 실행됐다.
     */
    public static DataInitMode from(String value) {
      try {
        return DataInitMode.valueOf(value.trim().toUpperCase());
      } catch (IllegalArgumentException | NullPointerException e) {
        throw new IllegalStateException(
            "chess.data-init.mode 값이 잘못되었습니다: '" + value + "'. 가능한 값: "
                + Arrays.toString(values())
                + ". YAML 에서 OFF 는 따옴표로 감싸야 합니다(mode: \"OFF\").", e);
      }
    }
  }

  public void run(String... args) throws Exception {

    DataInitMode mode = DataInitMode.from(initMode);
    initializeAdminUser();
    log.info("Data initialization mode: {}", mode);
    if (mode == DataInitMode.OFF) {
      log.info("Data initialization is OFF.");
      return;
    }

    if (mode == DataInitMode.CSV_TO_DB) {
      importFromCsv();
      return;
    }

    processPgn(mode);
  }

  private ChessPlayer getOrCreatePlayer(Player player) {
    return playerCache.computeIfAbsent(player.getName(),
        (name) -> {
          ChessPlayer chessPlayer = new ChessPlayer(name, player.getElo());
          chessPlayer.setId(playerCache.size());
          return chessPlayer;
        });
  }

  private void saveGameIndexes(CustomGame game, DataInitMode dataInitMode) {
    ChessPlayer whitePlayer = getOrCreatePlayer(game.getWhitePlayer());
    ChessPlayer blackPlayer = getOrCreatePlayer(game.getBlackPlayer());

    GameProcessor gameProcessor = gameProcessorObjectProvider.getObject()
        .initialize(game, whitePlayer, blackPlayer);

    if (dataInitMode == DataInitMode.PGN_TO_CSV) {
      gameProcessor.writeChessPlayerToCsv();
    }

    for (Move move : game.getHalfMoves()) {

      if (!gameProcessor.doMove(move) && dataInitMode == DataInitMode.CSV_TO_DB) {
        gameIndexRepository.removeGameIndexByGameOffset(game.getOffset());
        GameProcessor.removeGame(game);
      }

      if (gameProcessor.hasEnoughExampleGameWithPawnStructure()) {
        continue;
      }

      if (gameProcessor.pawnStructureIsMeaningful() && gameProcessor.boardHasNewPawnStructure()) {

        if (dataInitMode == DataInitMode.PGN_TO_DB) {
          gameProcessor.saveGameIndex();
        }

        if (dataInitMode == DataInitMode.PGN_TO_CSV) {
          gameProcessor.writeGameIndexToCsv();
        }
        gameProcessor.recordCurrentPawnStructure();
      }
    }
  }

  private void processPgn(DataInitMode mode)
      throws Exception {
    String path = Path.of(pgnPath).toFile().getAbsolutePath();

    int cnt = 0;
    playerCache = new HashMap<>();
    GameProcessor.resetProcessingState();

    //Ready for Export PGN to CSV
    if (mode == DataInitMode.PGN_TO_CSV) {
      Files.createDirectories(Path.of(csvDir));
      PrintWriter playerWriter = new PrintWriter(
          Files.newBufferedWriter(Path.of(csvDir, "players.csv")));
      PrintWriter gameIndexWriter = new PrintWriter(
          Files.newBufferedWriter(Path.of(csvDir, "game_indexes.csv")));
      GameProcessor.setPlayerWriter(playerWriter);
      GameProcessor.setGameIndexWriter(gameIndexWriter);
      log.info("Exporting PGN to CSV in: {}", csvDir);
    }

    CustomPgnIterator games = new CustomPgnIterator(path);
    for (CustomGame game : games) {

      if (game.isNotGMGame() || game.isNotClassicalFormat()) {
        continue;
      }

      saveGameIndexes(game, mode);
      cnt++;
    }
    if (mode == DataInitMode.PGN_TO_DB) {
      gameProcessorObjectProvider.getObject().saveAndFlushGameIndexes();
    }
    games.close();

    if (mode == DataInitMode.PGN_TO_DB) {
      gameIndexRepository.finishBulkInsert();
    } else {
      GameProcessor.close();
    }
    log.info("Data processing completed. Total game indexes: {}", cnt);
  }

  private void importFromCsv() {
    long startTime = System.currentTimeMillis();
    log.info("Importing data from CSV files in: {} using native DB commands", csvDir);
    Path playerCsv = Path.of(csvDir, "players.csv");
    Path gameIndexCsv = Path.of(csvDir, "game_indexes.csv");

    if (!playerCsv.toFile().exists() || !gameIndexCsv.toFile().exists()) {
      log.error("CSV files not found in {}. Skipping import.", csvDir);
      return;
    }

    log.info("Truncating chess_player table...");
    chessPlayerRepository.truncateTable();
    log.info("Importing players from CSV...");
    chessPlayerRepository.importFromCsv(playerCsv);
    log.info("Chess players imported.");

    try {
      log.info("Importing game indexes from CSV (this may take a minute)...");
      long loadStartTime = System.currentTimeMillis();
      gameIndexRepository.importFromCsv(gameIndexCsv);
      log.info("Game indexes raw data imported in {} ms.",
          System.currentTimeMillis() - loadStartTime);
    } finally {
      log.info("Finishing bulk insert (recreating indexes and syncing sequences)...");
      long indexStartTime = System.currentTimeMillis();
      gameIndexRepository.finishBulkInsert();
      log.info("Indexes recreated and sequences synced in {} ms.",
          System.currentTimeMillis() - indexStartTime);
    }
    log.info("Total CSV import completed in {} ms.", System.currentTimeMillis() - startTime);
  }

  protected void initializeAdminUser() {
    String username = normalizePropertyValue(adminUsername);
    String password = normalizePropertyValue(adminPassword);
    String email = normalizePropertyValue(adminEmail);

    User admin = userRepository.findUserByUsername(username);
    if (admin == null) {
      admin = userRepository.findUserByEmail(email);
    }
    if (admin == null && !username.equals(adminUsername)) {
      admin = userRepository.findUserByUsername(adminUsername);
    }
    if (admin == null && !email.equals(adminEmail)) {
      admin = userRepository.findUserByEmail(email);
    }

    if (admin == null) {
      admin = new User(email, username, passwordEncoder.encode(password), Role.ROLE_ADMIN);
      userRepository.save(admin);
      log.info("Admin user created: {}", username);
      return;
    }

    boolean changed = false;
    if (!username.equals(admin.getUsername())) {
      admin.setUsername(username);
      changed = true;
    }
    if (!email.equals(admin.getEmail())) {
      admin.setEmail(email);
      changed = true;
    }
    if (!passwordEncoder.matches(password, admin.getPassword())) {
      admin.setPassword(passwordEncoder.encode(password));
      changed = true;
    }
    if (admin.getRole() != Role.ROLE_ADMIN) {
      admin.setRole(Role.ROLE_ADMIN);
      changed = true;
    }

    if (changed) {
      userRepository.save(admin);
      log.info("Admin user updated: {}", username);
    }
  }

  private String normalizePropertyValue(String value) {
    String normalized = value.trim();
    if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
      return normalized.substring(1, normalized.length() - 1);
    }
    return normalized;
  }


}
