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
    for (int start = 0; start < names.size(); start += INSERT_CHUNK_SIZE) {
      int end = Math.min(start + INSERT_CHUNK_SIZE, names.size());
      
      // PostgreSQL specific: ON CONFLICT DO UPDATE ... RETURNING id, name
      // This allows us to get the ID even if the player was already inserted by another process
      // and eliminates the need for a separate SELECT.
      StringBuilder sql = new StringBuilder("INSERT INTO chess_player (name) VALUES ");
      List<Object> params = new ArrayList<>(end - start);
      for (int i = start; i < end; i++) {
        if (i > start) {
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

    for (ChessPlayer chessPlayer : missingPlayers.values()) {
      if (chessPlayer.getId() == 0) {
        throw new IllegalStateException("Failed to insert or retrieve ID for chess player: " + chessPlayer.getName());
      }
    }
  }
}
