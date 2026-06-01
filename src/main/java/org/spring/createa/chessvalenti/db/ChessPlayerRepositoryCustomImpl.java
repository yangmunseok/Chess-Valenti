package org.spring.createa.chessvalenti.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ChessPlayerRepositoryCustomImpl implements ChessPlayerRepositoryCustom {

  private static final int INSERT_CHUNK_SIZE = 1000;
  private static final int SELECT_CHUNK_SIZE = 1000;

  @Autowired
  JdbcTemplate jdbcTemplate;

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
    insertNames(names);
    fillIds(names, missingPlayers);
  }

  private void insertNames(List<String> names) {
    for (int start = 0; start < names.size(); start += INSERT_CHUNK_SIZE) {
      int end = Math.min(start + INSERT_CHUNK_SIZE, names.size());
      StringBuilder sql = new StringBuilder("insert into chess_player (name) values ");
      List<Object> params = new ArrayList<>(end - start);
      for (int i = start; i < end; i++) {
        if (i > start) {
          sql.append(", ");
        }
        sql.append("(?)");
        params.add(names.get(i));
      }
      jdbcTemplate.update(sql.toString(), params.toArray());
    }
  }

  private void fillIds(List<String> names, Map<String, ChessPlayer> missingPlayers) {
    for (int start = 0; start < names.size(); start += SELECT_CHUNK_SIZE) {
      int end = Math.min(start + SELECT_CHUNK_SIZE, names.size());
      StringBuilder sql = new StringBuilder(
          "select id, name from chess_player where name in (");
      List<Object> params = new ArrayList<>(end - start);
      for (int i = start; i < end; i++) {
        if (i > start) {
          sql.append(", ");
        }
        sql.append("?");
        params.add(names.get(i));
      }
      sql.append(")");

      jdbcTemplate.query(sql.toString(), rs -> {
        ChessPlayer chessPlayer = missingPlayers.get(rs.getString("name"));
        if (chessPlayer != null && chessPlayer.getId() == 0) {
          chessPlayer.setId(rs.getLong("id"));
        }
      }, params.toArray());
    }

    for (ChessPlayer chessPlayer : missingPlayers.values()) {
      if (chessPlayer.getId() == 0) {
        throw new IllegalStateException("Failed to insert chess player: " + chessPlayer.getName());
      }
    }
  }
}
