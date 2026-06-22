package org.spring.createa.chessvalenti.db;

import static java.nio.file.Files.newInputStream;

import com.github.bhlangonijr.chesslib.Board;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GameIndexRepositoryCustomImpl implements GameIndexRepositoryCustom {

  private static final String PAWN_SORT_INDEX = "pawn_sort_idx";
  private static final int INSERT_CHUNK_SIZE = 1000;

  @Autowired
  @Lazy
  GameIndexRepository gameIndexRepository;

  @Autowired
  ChessHashHelper chessHashHelper;

  @Autowired
  JdbcTemplate jdbcTemplate;

  private String cachedDatabaseProductName;

  private String getDatabaseProductName() {
    if (cachedDatabaseProductName == null) {
      try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
        cachedDatabaseProductName = conn.getMetaData().getDatabaseProductName();
      } catch (SQLException e) {
        throw new RuntimeException("Could not determine database product name", e);
      }
    }
    return cachedDatabaseProductName;
  }

  private boolean isPostgreSQL() {
    return getDatabaseProductName().equalsIgnoreCase("PostgreSQL");
  }

  public List<GameIndex> findAllByPawnStructure(Board board) {
    System.out.println("hashed pawn");
    System.out.println(ChessHashHelper.hashPawnStructure(board));
    return gameIndexRepository.findAllByPawnStructure(chessHashHelper.hashPawnStructure(board));
  }

  public List<GameIndex> findAllByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn) {
    long hashedPawnStructure = ChessHashHelper.hashPawnStructure(board);
    int hashedPieceConfiguration = ChessHashHelper.hashPieceConfiguration(wq, wr, wb, wn, bq, br,
        bb, bn);
    return gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(hashedPawnStructure,
        hashedPieceConfiguration);
  }

  @Override
  public Page<GameIndex> findByPawnStructure(Board board, Pageable pageable) {
    return gameIndexRepository.findAllByPawnStructure(ChessHashHelper.hashPawnStructure(board),
        pageable);
  }

  @Override
  public Page<GameIndex> findByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn, Pageable pageable) {
    long hashedPawnStructure = ChessHashHelper.hashPawnStructure(board);
    int hashedPieceConfiguration = ChessHashHelper.hashPieceConfiguration(wq, wr, wb, wn, bq, br,
        bb, bn);
    return gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(hashedPawnStructure,
        hashedPieceConfiguration, pageable);
  }

  @Override
  public void importFromCsv(Path path) {
    if (isPostgreSQL()) {
      importFromCsvPostgreSQL(path);
    } else {
      importFromCsvMySQL(path);
    }
  }

  private void importFromCsvPostgreSQL(Path path) {
    jdbcTemplate.execute((Connection conn) -> {
      PGConnection pgConn = conn.unwrap(PGConnection.class);
      CopyManager copyManager = pgConn.getCopyAPI();
      try (InputStream in = newInputStream(path)) {
        copyManager.copyIn("""
            COPY game_index (
              id,
              pawn_structure,
              piece_configuration,
              game_offset,
              move_index,
              white_player_id,
              black_player_id,
              white_elo,
              black_elo,
              max_elo,
              total_elo
            ) FROM STDIN WITH (FORMAT csv, QUOTE '\"', ESCAPE '\"')
            """, in);
      } catch (java.io.IOException e) {
        throw new SQLException("Error during PostgreSQL COPY", e);
      }
      return null;
    });
  }

  private void importFromCsvMySQL(Path path) {
    String absolutePath = path.toAbsolutePath().toString().replace("\\", "/");
    String sql = "LOAD DATA LOCAL INFILE '" + absolutePath + "' " +
        "INTO TABLE game_index " +
        "FIELDS TERMINATED BY ',' " +
        "OPTIONALLY ENCLOSED BY '\"' " +
        "(id,pawn_structure, piece_configuration, game_offset, move_index, " +
        "white_player_id, black_player_id, white_elo, black_elo, max_elo, total_elo)";
    jdbcTemplate.execute(sql);
  }

  @Override
  @Transactional
  public void insertAll(List<GameIndex> gameIndexList) {
    if (gameIndexList.isEmpty()) {
      return;
    }
    gameIndexRepository.saveAll(gameIndexList);
    gameIndexRepository.flush();
  }

  @Override
  public void truncateTable() {
    if (isPostgreSQL()) {
      jdbcTemplate.execute("TRUNCATE TABLE game_index RESTART IDENTITY");
    } else {
      jdbcTemplate.execute("TRUNCATE TABLE game_index");
    }
  }

  @Override
  public void finishBulkInsert() {
    syncSequenceWithTable();
  }

  private void syncSequenceWithTable() {
    if (isPostgreSQL()) {
      jdbcTemplate.execute("""
          select setval('game_index_seq', (select coalesce(max(id), 0) + 1 from game_index), false)
          """);
    } else {
      jdbcTemplate.update("""
          update game_index_seq
          set next_val = greatest(
            next_val,
            coalesce((select max_id from (select max(id) + 1 as max_id from game_index) ids), 1)
          )
          """);
    }
  }
}
