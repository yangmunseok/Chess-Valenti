package org.spring.createa.chessvalenti.config;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.game.Player;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.spring.createa.chessvalenti.db.ChessPlayerRepository;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.dto.game.CustomGame;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.spring.createa.chessvalenti.util.pgn.CustomPgnIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  @Autowired
  private final GameIndexRepository gameIndexRepository;
  @Autowired
  private final ChessHashHelper chessHashHelper;
  @Autowired
  private final EntityManager em;
  @Autowired
  private final ChessPlayerRepository chessPlayerRepository;

  private final boolean skip = false;

  public void run(String... args) throws Exception {

    if (skip) {
      return;
    }
    String pgnPath = "static/pgn/AJ-OTB-PGN-001.pgn";
    ClassPathResource resource = new ClassPathResource(pgnPath);
    String path = resource.getFile().getAbsolutePath();
    CustomPgnIterator games = new CustomPgnIterator(path);
    int batchSize = 3000;
    int cnt = 0;
    List<GameIndex> gameIndexList = new ArrayList<>();
    Map<String, ChessPlayer> playerCache = new HashMap<>();

    for (CustomGame game : games) {
      game.loadMoveText();
      MoveList moves = game.getHalfMoves();
      //then 4 MB/s

      Set<Long> visited = new HashSet<>();
      Board board = new Board();
      visited.add(chessHashHelper.hashPawnStructure(board));

      int white_elo = game.getWhitePlayer().getElo();
      int black_elo = game.getBlackPlayer().getElo();
      ChessPlayer whitePlayer = savePlayer(game.getWhitePlayer(), playerCache);
      ChessPlayer blackPlayer = savePlayer(game.getBlackPlayer(), playerCache);
      long game_offset = game.getOffset();
      long move_idx = 0;
      String fen = board.getFen();
      if (fen != null) {
        if (!fen.equals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")) {
          continue;
        }
      }

      for (Move move : moves) {
        try {
          board.doMove(move);
        } catch (Exception e) {
          System.out.println("invalid pgn");
          gameIndexRepository.removeGameIndexByGameOffset(game_offset);
          gameIndexList.removeIf((gameIndex -> gameIndex.getGameOffset() == game_offset));
          break;
        }
        //then 3.5 MB/s

        move_idx++;
        long key = chessHashHelper.hashPawnStructure(board);
        if (visited.contains(key)) {
          continue;
        }

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
        visited.add(key);

        cnt++;
        //then 3.25 MB/s
        GameIndex gameIndex = new GameIndex(key, hashedPieceConfiguration, game_offset, move_idx,
            whitePlayer, blackPlayer, white_elo, black_elo);
        gameIndexList.add(gameIndex);
        if (cnt % batchSize == 0) {
          gameIndexRepository.saveAll(gameIndexList);
          //System.out.println("saveAll invoked");
          gameIndexList.clear();
          em.clear();
        }

        //then 1.5 MB/s

      }
    }
    gameIndexRepository.saveAll(gameIndexList);
  }

  private ChessPlayer savePlayer(Player player, Map<String, ChessPlayer> playerCache) {
    String name = player.getName();
    String key = name;
    ChessPlayer cached = playerCache.get(key);
    if (cached != null) {
      return cached;
    }

    ChessPlayer chessPlayer = chessPlayerRepository.findFirstByName(name)
        .orElseGet(() -> chessPlayerRepository.save(
            new ChessPlayer(name)));
    playerCache.put(key, chessPlayer);
    return chessPlayer;
  }

}
