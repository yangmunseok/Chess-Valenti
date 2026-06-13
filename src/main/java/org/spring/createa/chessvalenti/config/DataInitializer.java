package org.spring.createa.chessvalenti.config;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.spring.createa.chessvalenti.util.ChessBoardUtil;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.spring.createa.chessvalenti.util.pgn.CustomPgnIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
  private final PlatformTransactionManager transactionManager;
  private final ChessBoardUtil chessBoardUtil;

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
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(status -> {
      initializeAdminUser();
      return null;
    });

    DataInitMode mode = DataInitMode.from(initMode);
    if (mode == DataInitMode.OFF) {
      log.info("Data initialization is OFF.");
      return;
    }

    if (mode == DataInitMode.CSV_TO_DB) {
      importFromCsv(transactionTemplate);
      return;
    }

    processPgn(mode, transactionTemplate);
  }

  private void processPgn(DataInitMode mode, TransactionTemplate transactionTemplate)
      throws Exception {
    boolean toDb = mode == DataInitMode.PGN_TO_DB;
    String path = Path.of(pgnPath).toFile().getAbsolutePath();

    int batchSize = 10000;
    int cnt = 0;
    int maxGame = 50;
    int engineRating = 2900;
    int gmRating = 2500;
    List<GameIndex> gameIndexList = new ArrayList<>(batchSize);
    Map<String, ChessPlayer> playerCache = toDb ? loadPlayerCache() : new HashMap<>();
    Map<Long, Integer> pawnStructureCnt = new HashMap<>();

    PrintWriter playerWriter = null;
    PrintWriter gameIndexWriter = null;
    long nextPlayerId = 1;
    long nextGameIndexId = 1;

    if (!toDb) {
      Files.createDirectories(Path.of(csvDir));
      playerWriter = new PrintWriter(Files.newBufferedWriter(Path.of(csvDir, "players.csv")));
      gameIndexWriter = new PrintWriter(
          Files.newBufferedWriter(Path.of(csvDir, "game_indexes.csv")));
      log.info("Exporting PGN to CSV in: {}", csvDir);
    } else {
      gameIndexRepository.prepareForBulkInsert();
    }

    try {
      try (CustomPgnIterator games = new CustomPgnIterator(path)) {
        for (CustomGame game : games) {
          int pawnMoveCnt = 0;
          int whiteElo = game.getWhitePlayer().getElo();
          int blackElo = game.getBlackPlayer().getElo();

          if (Math.max(whiteElo, blackElo) > engineRating
              || Math.max(whiteElo, blackElo) < gmRating) {
            continue;
          }

          String initialFen = game.getFen();
          if (initialFen != null && !initialFen.equals(DEFAULT_STARTING_FEN)) {
            continue;
          }

          MoveList moves = game.getHalfMoves();

          Set<Long> visited = new HashSet<>(128);
          Board board = new Board();
          visited.add(chessHashHelper.hashPawnStructure(board));

          int white_elo = game.getWhitePlayer().getElo();
          int black_elo = game.getBlackPlayer().getElo();

          ChessPlayer whitePlayer;
          ChessPlayer blackPlayer;

          if (toDb) {
            whitePlayer = savePlayer(game.getWhitePlayer(), playerCache);
            blackPlayer = savePlayer(game.getBlackPlayer(), playerCache);
          } else {
            whitePlayer = getOrAssignPlayer(game.getWhitePlayer().getName(), playerCache,
                playerWriter, new long[]{nextPlayerId});
            if (whitePlayer.getId() == nextPlayerId) {
              nextPlayerId++;
            }
            blackPlayer = getOrAssignPlayer(game.getBlackPlayer().getName(), playerCache,
                playerWriter, new long[]{nextPlayerId});
            if (blackPlayer.getId() == nextPlayerId) {
              nextPlayerId++;
            }
          }

          long game_offset = game.getOffset();
          long move_idx = 0;

          for (Move move : moves) {
            try {
              Piece piece = board.getPiece(move.getFrom());

              if (piece == Piece.BLACK_PAWN || piece == Piece.WHITE_PAWN) {
                pawnMoveCnt++;
              }

              board.doMove(move);

              if (pawnMoveCnt < 6 || !chessBoardUtil.isMaterialEven(board)
                  || chessBoardUtil.countCenterPawns(board) < 4) {
                move_idx++;
                continue;
              }
            } catch (Exception e) {
              if (toDb) {
                gameIndexRepository.removeGameIndexByGameOffset(game_offset);
                gameIndexList.removeIf((gameIndex -> gameIndex.getGameOffset() == game_offset));
              }
              break;
            }

            move_idx++;
            long key = chessHashHelper.hashPawnStructure(board);
            if (!visited.add(key)) {
              continue;
            }
            int game_num = pawnStructureCnt.getOrDefault(key, 0);

            if (game_num >= maxGame) {
              continue;
            }

            pawnStructureCnt.put(key, game_num + 1);

            int wq = Long.bitCount(board.getBitboard(Piece.WHITE_QUEEN));
            int wr = Long.bitCount(board.getBitboard(Piece.WHITE_ROOK));
            int wb = Long.bitCount(board.getBitboard(Piece.WHITE_BISHOP));
            int wn = Long.bitCount(board.getBitboard(Piece.WHITE_KNIGHT));
            int bq = Long.bitCount(board.getBitboard(Piece.BLACK_QUEEN));
            int br = Long.bitCount(board.getBitboard(Piece.BLACK_ROOK));
            int bb = Long.bitCount(board.getBitboard(Piece.BLACK_BISHOP));
            int bn = Long.bitCount(board.getBitboard(Piece.BLACK_KNIGHT));

            int hashedPieceConfiguration = chessHashHelper.hashPieceConfiguration(wq, wr, wb, wn,
                bq, br, bb, bn);
            cnt++;

            GameIndex gameIndex = new GameIndex(key, hashedPieceConfiguration, game_offset,
                move_idx, whitePlayer, blackPlayer, white_elo, black_elo);

            if (toDb) {
              gameIndexList.add(gameIndex);
              if (gameIndexList.size() >= batchSize) {
                flushGameIndexesInTransaction(gameIndexList, transactionTemplate);
              }
            } else {
              gameIndex.setId(nextGameIndexId++);
              writeGameIndexToCsv(gameIndex, gameIndexWriter);
            }
          }
        }
      }
      if (toDb) {
        flushGameIndexesInTransaction(gameIndexList, transactionTemplate);
      }
    } finally {
      if (toDb) {
        gameIndexRepository.finishBulkInsert();
      } else {
        if (playerWriter != null) {
          playerWriter.close();
        }
        if (gameIndexWriter != null) {
          gameIndexWriter.close();
        }
      }
    }
    log.info("Data processing completed. Total game indexes: {}", cnt);
  }

  private ChessPlayer getOrAssignPlayer(String name, Map<String, ChessPlayer> playerCache,
      PrintWriter writer, long[] nextId) {
    ChessPlayer player = playerCache.get(name);
    if (player == null) {
      player = new ChessPlayer(name);
      player.setId(nextId[0]);
      playerCache.put(name, player);
      writer.print(player.getId() + ",\"" + name.replace("\"", "\"\"") + "\"\n");
    }
    return player;
  }

  private void writeGameIndexToCsv(GameIndex gi, PrintWriter writer) {
    // Column order: id, pawn_structure, piece_configuration, game_offset, move_index, 
    // white_player_id, black_player_id, white_elo, black_elo, max_elo, total_elo
    writer.print(gi.getId() + "," +
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

  private void importFromCsv(TransactionTemplate transactionTemplate) throws Exception {
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

    log.info("Preparing game_index for bulk insert (truncating and dropping indexes)...");
    gameIndexRepository.prepareForBulkInsert();

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

  private String[] parseCsvLine(String line) {
    List<String> result = new ArrayList<>();
    boolean inQuotes = false;
    StringBuilder sb = new StringBuilder();
    for (char c : line.toCharArray()) {
      if (c == '\"') {
        inQuotes = !inQuotes;
      } else if (c == ',' && !inQuotes) {
        result.add(sb.toString());
        sb.setLength(0);
      } else {
        sb.append(c);
      }
    }
    result.add(sb.toString());
    return result.toArray(new String[0]);
  }

  private Map<Long, ChessPlayer> loadPlayerCacheById() {
    Map<Long, ChessPlayer> playerCache = new HashMap<>();
    for (ChessPlayer chessPlayer : chessPlayerRepository.findAll()) {
      playerCache.put(chessPlayer.getId(), chessPlayer);
    }
    return playerCache;
  }

  private void flushGameIndexesInTransaction(List<GameIndex> gameIndexList,
      TransactionTemplate transactionTemplate) {
    if (gameIndexList.isEmpty()) {
      return;
    }
    transactionTemplate.execute(status -> {
      flushGameIndexes(gameIndexList);
      return null;
    });
  }

  private void initializeAdminUser() {
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

  private ChessPlayer savePlayer(com.github.bhlangonijr.chesslib.game.Player player,
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

  private void flushGameIndexes(List<GameIndex> gameIndexList) {
    if (gameIndexList.isEmpty()) {
      return;
    }
    chessPlayerRepository.insertMissingAndFillIds(collectPlayers(gameIndexList));
    gameIndexRepository.insertAll(gameIndexList);
    gameIndexList.clear();
  }

  private List<ChessPlayer> collectPlayers(List<GameIndex> gameIndexList) {
    Map<String, ChessPlayer> players = new HashMap<>();
    for (GameIndex gameIndex : gameIndexList) {
      ChessPlayer whitePlayer = gameIndex.getWhitePlayer();
      ChessPlayer blackPlayer = gameIndex.getBlackPlayer();
      if (whitePlayer.getId() == 0) {
        players.putIfAbsent(whitePlayer.getName(), whitePlayer);
      }
      if (blackPlayer.getId() == 0) {
        players.putIfAbsent(blackPlayer.getName(), blackPlayer);
      }
    }
    return new ArrayList<>(players.values());
  }

}
