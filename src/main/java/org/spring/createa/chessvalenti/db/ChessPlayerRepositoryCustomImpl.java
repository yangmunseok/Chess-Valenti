package org.spring.createa.chessvalenti.db;

import static java.nio.file.Files.newInputStream;

import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ChessPlayerRepositoryCustomImpl implements ChessPlayerRepositoryCustom {

  private static final int INSERT_CHUNK_SIZE = 1000;

  @Autowired
  JdbcTemplate jdbcTemplate;

  private String cachedDatabaseProductName;

  private String getDatabaseProductName() {
    if (cachedDatabaseProductName == null) {
      try (java.sql.Connection conn = jdbcTemplate.getDataSource().getConnection()) {
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

  @Override
  @Transactional
  public void insertMissingAndFillIds(Collection<ChessPlayer> chessPlayers) {
    Map<String, ChessPlayer> missingPlayers = new LinkedHashMap<>();
    for (ChessPlayer chessPlayer : chessPlayers) {
      if (chessPlayer.getId() == 0) {
        missingPlayers.putIfAbsent(chessPlayer.getName(), chessPlayer);
      }
    }
    if (missingPlayers.isEmpty()) {
      return;
    }

    List<String> names = new ArrayList<>(missingPlayers.keySet());
    for (int start = 0; start < names.size(); start += INSERT_CHUNK_SIZE) {
      int end = Math.min(start + INSERT_CHUNK_SIZE, names.size());

      if (isPostgreSQL()) {
        insertMissingAndFillIdsPostgreSQL(missingPlayers, names.subList(start, end));
      } else {
        insertMissingAndFillIdsMySQL(missingPlayers, names.subList(start, end));
      }
    }

    for (ChessPlayer chessPlayer : missingPlayers.values()) {
      if (chessPlayer.getId() == 0) {
        throw new IllegalStateException(
            "Failed to insert or retrieve ID for chess player: " + chessPlayer.getName());
      }
    }
  }

  @Override
  @Transactional
  public void insertAll(List<ChessPlayer> players) {
    if (players.isEmpty()) {
      return;
    }

    for (int start = 0; start < players.size(); start += INSERT_CHUNK_SIZE) {
      int end = Math.min(start + INSERT_CHUNK_SIZE, players.size());
      insertChunk(players.subList(start, end));
    }
    syncSequenceWithTable();
  }

  private void insertChunk(List<ChessPlayer> players) {
    StringBuilder sql = new StringBuilder("INSERT INTO chess_player (id, name) VALUES ");
    List<Object> params = new ArrayList<>(players.size() * 2);
    for (int i = 0; i < players.size(); i++) {
      if (i > 0) {
        sql.append(", ");
      }
      sql.append("(?, ?)");
      ChessPlayer player = players.get(i);
      params.add(player.getId());
      params.add(player.getName());
    }

    if (isPostgreSQL()) {
      sql.append(" ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name");
    } else {
      sql.append(" ON DUPLICATE KEY UPDATE name = VALUES(name)");
    }

    jdbcTemplate.update(sql.toString(), params.toArray());
  }

  @Override
  public void truncateTable() {
    if (isPostgreSQL()) {
      jdbcTemplate.execute("TRUNCATE TABLE chess_player RESTART IDENTITY CASCADE");
    } else {
      jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
      jdbcTemplate.execute("TRUNCATE TABLE chess_player");
      jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
  }

  @Override
  public void importFromCsv(java.nio.file.Path path) {
    if (isPostgreSQL()) {
      importFromCsvPostgreSQL(path);
    } else {
      importFromCsvMySQL(path);
    }
    syncSequenceWithTable();
  }

  private void importFromCsvPostgreSQL(Path path) {
    jdbcTemplate.execute((Connection conn) -> {
      PGConnection pgConn = conn.unwrap(PGConnection.class);
      CopyManager copyManager = pgConn.getCopyAPI();
      try (InputStream in = newInputStream(path)) {
        copyManager.copyIn(
            "COPY chess_player (id, name) FROM STDIN WITH (FORMAT csv, QUOTE '\"', ESCAPE '\"')",
            in);
      } catch (java.io.IOException e) {
        throw new SQLException("Error during PostgreSQL COPY", e);
      }
      return null;
    });
  }

  private void importFromCsvMySQL(java.nio.file.Path path) {
    String absolutePath = path.toAbsolutePath().toString().replace("\\", "/");
    String sql = "LOAD DATA LOCAL INFILE '" + absolutePath + "' " +
        "INTO TABLE chess_player " +
        "FIELDS TERMINATED BY ',' " +
        "OPTIONALLY ENCLOSED BY '\"' " +
        "(id, name)";
    jdbcTemplate.execute(sql);
  }


  private void syncSequenceWithTable() {
    if (isPostgreSQL()) {
      jdbcTemplate.execute("""
          SELECT setval(pg_get_serial_sequence('chess_player', 'id'), 
                 (SELECT COALESCE(MAX(id), 0) + 1 FROM chess_player), false)
          """);
    } else {
      Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM chess_player",
          Long.class);
      jdbcTemplate.execute("ALTER TABLE chess_player AUTO_INCREMENT = " + maxId);
    }
  }

  private void insertMissingAndFillIdsPostgreSQL(Map<String, ChessPlayer> missingPlayers,
      List<String> names) {
    StringBuilder sql = new StringBuilder("INSERT INTO chess_player (name) VALUES ");
    List<Object> params = new ArrayList<>(names.size());
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        sql.append(", ");
      }
      sql.append("(?)");
      params.add(names.get(i));
    }
    sql.append(" ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id, name");

    jdbcTemplate.query(sql.toString(), rs -> {
      String name = rs.getString("name");
      long id = rs.getLong("id");
      ChessPlayer player = missingPlayers.get(name);
      if (player != null) {
        player.setId(id);
      }
    }, params.toArray());
  }

  private void insertMissingAndFillIdsMySQL(Map<String, ChessPlayer> missingPlayers,
      List<String> names) {
    StringBuilder sql = new StringBuilder("INSERT INTO chess_player (name) VALUES ");
    List<Object> params = new ArrayList<>(names.size());
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        sql.append(", ");
      }
      sql.append("(?)");
      params.add(names.get(i));
    }
    sql.append(" ON DUPLICATE KEY UPDATE name = VALUES(name)");
    jdbcTemplate.update(sql.toString(), params.toArray());

    // For MySQL, we need a separate SELECT to get the IDs
    StringBuilder selectSql = new StringBuilder(
        "SELECT id, name FROM chess_player WHERE name IN (");
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        selectSql.append(", ");
      }
      selectSql.append("?");
    }
    selectSql.append(")");

    jdbcTemplate.query(selectSql.toString(), rs -> {
      String name = rs.getString("name");
      long id = rs.getLong("id");
      ChessPlayer player = missingPlayers.get(name);
      if (player != null) {
        player.setId(id);
      }
    }, names.toArray());
  }
}
