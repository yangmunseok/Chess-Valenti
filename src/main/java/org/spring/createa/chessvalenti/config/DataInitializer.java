package org.spring.createa.chessvalenti.config;

import com.github.bhlangonijr.chesslib.game.Player;
import com.github.bhlangonijr.chesslib.move.Move;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.ChessPlayerRepository;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.CustomGame;
import org.spring.createa.chessvalenti.service.GameService;
import org.spring.createa.chessvalenti.util.ChessBoardUtil;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.spring.createa.chessvalenti.util.GameProcessor;
import org.spring.createa.chessvalenti.util.pgn.CustomPgnIterator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
  private final ChessBoardUtil chessBoardUtil;
  private final GameService gameService;
  ObjectProvider<GameProcessor> gameProcessorObjectProvider;

  Map<String, ChessPlayer> playerCache;

  @Value("${chess.pgn.path}")
  private String pgnPath;

  @Value("${admin.username}")
  private String adminUsername;

  @Value("${admin.password}")
  private String adminPassword;

  @Value("${admin.email}")
  private String adminEmail;

  @Value("${chess.data-init.mode:PGN_TO_DB}")
  private String initMode;

  @Value("${chess.data-init.csv-dir:data/csv}")
  private String csvDir;

  public enum DataInitMode {
    PGN_TO_DB,
    PGN_TO_CSV,
    CSV_TO_DB,
    OFF;

    public static DataInitMode from(String value) {
      try {
        return DataInitMode.valueOf(value.toUpperCase());
      } catch (Exception e) {
        return PGN_TO_DB;
      }
    }
  }

  public void run(String... args) throws Exception {

    initializeAdminUser();
    DataInitMode mode = DataInitMode.from(initMode);
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

    GameProcessor gameProcessor = new GameProcessor(chessBoardUtil, game, whitePlayer,
        blackPlayer);

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
      }
    }
  }

  private void processPgn(DataInitMode mode)
      throws Exception {
    boolean toDb = mode == DataInitMode.PGN_TO_DB;
    String path = Path.of(pgnPath).toFile().getAbsolutePath();

    int cnt = 0;
    playerCache = toDb ? loadPlayerCache() : new HashMap<>();

    //Ready for Export PGN to CSV
    if (!toDb) {
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
    if (toDb) {
      GameProcessor gameProcessor = new GameProcessor();
      gameProcessor.saveAndFlushGameIndexes();
    }
    games.close();

    if (toDb) {
      gameIndexRepository.finishBulkInsert();
    } else {
      GameProcessor.close();
    }
    log.info("Data processing completed. Total game indexes: {}", cnt);
  }

  //플레이어를 캐시해서 찾아보고 없으면 생성한뒤 파일에 넣는다.
  private ChessPlayer getOrAssignPlayer(String name, Map<String, ChessPlayer> playerCache,
      PrintWriter writer, long nextId) {
    ChessPlayer player = playerCache.get(name);
    if (player == null) {
      player = new ChessPlayer(name);
      player.setId(nextId);
      playerCache.put(name, player);
      writer.print(player.getId() + ",\"" + name.replace("\"", "\"\"") + "\"\n");
    }
    return player;
  }

  private void writeGameIndexToCsv(GameIndex gi, PrintWriter writer) {
    // Column order: id, pawn_structure, piece_configuration, game_offset, move_index, 
    // white_player_id, black_player_id, white_elo, black_elo, max_elo, total_elo
    writer.print(
        gi.getPawnStructure() + "," +
            gi.getPieceConfiguration() + "," +
            gi.getGameOffset() + "," +
            gi.getMoveIndex() + "," +
            gi.getWhitePlayer().getId() + "," +
            gi.getBlackPlayer().getId() + "," +
            gi.getWhiteElo() + "," +
            gi.getBlackElo() + "," +
            gi.getMaxElo() + "," +
            gi.getTotalElo() + "\n");
  }

  private void writeChessPlayerToCsv(ChessPlayer player, PrintWriter writer) {
    writer.print(
        player.getId() + ",\"" + player.getName().replace("\"", "\"\"")
            + ",\"" + player.getRating() + "\"\n");
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

  @Transactional
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

  private Map<String, ChessPlayer> loadPlayerCache() {
    Map<String, ChessPlayer> playerCache = new HashMap<>();
    for (ChessPlayer chessPlayer : chessPlayerRepository.findAll()) {
      playerCache.putIfAbsent(chessPlayer.getName(), chessPlayer);
    }
    return playerCache;
  }

  //플레이어를 캐시해서 찾아보고 없으면 생성한뒤 캐시에 넣는다.
  private ChessPlayer savePlayer(Player player,
      Map<String, ChessPlayer> playerCache) {
    String name = player.getName();
    String key = name;
    ChessPlayer cached = playerCache.get(key);
    if (cached != null) {
      return cached;
    }

    ChessPlayer chessPlayer = new ChessPlayer(name);
    playerCache.put(key, chessPlayer);
    return chessPlayer;
  }

}
