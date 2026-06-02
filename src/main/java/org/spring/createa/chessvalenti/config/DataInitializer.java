package org.spring.createa.chessvalenti.config;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.game.Player;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.spring.createa.chessvalenti.db.ChessPlayerRepository;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.CustomGame;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.spring.createa.chessvalenti.util.pgn.CustomPgnIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private static final String DEFAULT_STARTING_FEN =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  private final GameIndexRepository gameIndexRepository;
  private final ChessHashHelper chessHashHelper;
  private final ChessPlayerRepository chessPlayerRepository;
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Value("${chess.pgn.path}")
  private String pgnPath;

  @Value("${admin.username}")
  private String adminUsername;

  @Value("${admin.password}")
  private String adminPassword;

  @Value("${admin.email}")
  private String adminEmail;
  
  public void run(String... args) throws Exception {
    initializeAdminUser();

    String path = Path.of(pgnPath).toFile().getAbsolutePath();

    int batchSize = 20000;
    int cnt = 0;
    int maxGame = 200;
    int engineRating = 2900;
    int gmRating = 2500;
    List<GameIndex> gameIndexList = new ArrayList<>(batchSize);
    Map<String, ChessPlayer> playerCache = loadPlayerCache();
    Map<Long, Integer> pawnStructureCnt = new HashMap<>();
    gameIndexRepository.prepareForBulkInsert();
    try {
      try (CustomPgnIterator games = new CustomPgnIterator(path)) {
        for (CustomGame game : games) {
          int whiteElo = game.getWhitePlayer().getElo();
          int blackElo = game.getBlackPlayer().getElo();

          //filter game
          if (Math.max(whiteElo, blackElo) > engineRating
              || Math.max(whiteElo, blackElo) < gmRating) {
            continue;
          }

          String initialFen = game.getFen();
          if (initialFen != null && !initialFen.equals(DEFAULT_STARTING_FEN)) {
            continue;
          }
          MoveList moves = game.getHalfMoves();
          //then 4 MB/s

          Set<Long> visited = new HashSet<>(128);
          Board board = new Board();
          visited.add(chessHashHelper.hashPawnStructure(board));

          int white_elo = game.getWhitePlayer().getElo();
          int black_elo = game.getBlackPlayer().getElo();
          ChessPlayer whitePlayer = savePlayer(game.getWhitePlayer(), playerCache);
          ChessPlayer blackPlayer = savePlayer(game.getBlackPlayer(), playerCache);
          long game_offset = game.getOffset();
          long move_idx = 0;

          for (Move move : moves) {
            try {
              board.doMove(move);
            } catch (Exception e) {
              System.out.println("invalid pgn");
              gameIndexRepository.removeGameIndexByGameOffset(game_offset);
              gameIndexList.removeIf((gameIndex -> gameIndex.getGameOffset() == game_offset));
              break;
            }
            //then 3.5 MB

            move_idx++;
            long key = chessHashHelper.hashPawnStructure(board);
            if (!visited.add(key)) {
              continue;
            }
            int game_num = pawnStructureCnt.getOrDefault(key, 0);

            if (game_num > maxGame) {
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
            //then 3.25 MB/s

            GameIndex gameIndex = new GameIndex(key, hashedPieceConfiguration, game_offset,
                move_idx, whitePlayer, blackPlayer, white_elo, black_elo);
            gameIndexList.add(gameIndex);
            if (cnt % batchSize == 0) {

              flushGameIndexes(gameIndexList);
            }

            //then 1.5 MB/s
          }


        }
      }
      flushGameIndexes(gameIndexList);
    } finally {
      gameIndexRepository.finishBulkInsert();
    }
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
      admin = userRepository.findUserByEmail(adminEmail);
    }

    if (admin == null) {
      admin = new User(email, username, passwordEncoder.encode(password), Role.ROLE_ADMIN);
      userRepository.save(admin);
      System.out.println("Admin user created: " + username);
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
      System.out.println("Admin user updated: " + username);
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

  private ChessPlayer savePlayer(Player player, Map<String, ChessPlayer> playerCache) {
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
