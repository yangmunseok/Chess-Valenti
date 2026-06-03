package org.spring.createa.chessvalenti.db;

import com.github.bhlangonijr.chesslib.Board;
import java.util.ArrayList;
import java.util.List;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GameIndexRepositoryCustomImpl implements GameIndexRepositoryCustom {

  private static final String PAWN_SORT_INDEX = "pawn_sort_idx";
  private static final String PAWN_PIECE_SORT_INDEX = "pawn_piece_sort_idx";
  private static final String LEGACY_PAWN_PIECE_CONFIGURATION_INDEX =
      "pawn_piece_configuration_idx";
  private static final int INSERT_CHUNK_SIZE = 1000;

  @Autowired
  @Lazy
  GameIndexRepository gameIndexRepository;

  @Autowired
  ChessHashHelper chessHashHelper;

  @Autowired
  JdbcTemplate jdbcTemplate;

  public List<GameIndex> findAllByPawnStructure(Board board) {
    System.out.println("hashed pawn");
    System.out.println(chessHashHelper.hashPawnStructure(board));
    return gameIndexRepository.findAllByPawnStructure(chessHashHelper.hashPawnStructure(board));
  }

  public List<GameIndex> findAllByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn) {
    long hashedPawnStructure = chessHashHelper.hashPawnStructure(board);
    int hashedPieceConfiguration = chessHashHelper.hashPieceConfiguration(wq, wr, wb, wn, bq, br,
        bb, bn);
    return gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(hashedPawnStructure,
        hashedPieceConfiguration);
  }

  @Override
  public Page<GameIndex> findByPawnStructure(Board board, Pageable pageable) {
    return gameIndexRepository.findAllByPawnStructure(chessHashHelper.hashPawnStructure(board),
        pageable);
  }

  @Override
  public Page<GameIndex> findByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn, Pageable pageable) {
    long hashedPawnStructure = chessHashHelper.hashPawnStructure(board);
    int hashedPieceConfiguration = chessHashHelper.hashPieceConfiguration(wq, wr, wb, wn, bq, br,
        bb, bn);
    return gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(hashedPawnStructure,
        hashedPieceConfiguration, pageable);
  }

  @Override
  public void prepareForBulkInsert() {
    truncateTable();
    dropIndexIfExists(PAWN_SORT_INDEX);
    dropIndexIfExists(PAWN_PIECE_SORT_INDEX);
    dropIndexIfExists(LEGACY_PAWN_PIECE_CONFIGURATION_INDEX);
  }

  private void truncateTable() {
    jdbcTemplate.execute("truncate table game_index restart identity cascade");
  }

  @Override
  @Transactional
  public void insertAll(List<GameIndex> gameIndexes) {
    if (gameIndexes.isEmpty()) {
      return;
    }

    long firstId = reserveIds(gameIndexes.size());
    for (int start = 0; start < gameIndexes.size(); start += INSERT_CHUNK_SIZE) {
      int end = Math.min(start + INSERT_CHUNK_SIZE, gameIndexes.size());
      insertChunk(gameIndexes, firstId, start, end);
    }
  }

  @Override
  public void finishBulkInsert() {
    createIndexIfMissing(PAWN_SORT_INDEX, """
        create index pawn_sort_idx
        on game_index (pawn_structure, max_elo desc, total_elo desc, id desc)
        """);
    createIndexIfMissing(PAWN_PIECE_SORT_INDEX, """
        create index pawn_piece_sort_idx
        on game_index (
          pawn_structure,
          piece_configuration,
          max_elo desc,
          total_elo desc,
          id desc
        )
        """);
    syncSequenceWithTable();
  }

  private void dropIndexIfExists(String indexName) {
    jdbcTemplate.execute("drop index if exists " + indexName);
  }

  private void createIndexIfMissing(String indexName, String sql) {
    jdbcTemplate.execute(sql);
  }

  private boolean indexExists(String indexName) {
    Integer count = jdbcTemplate.queryForObject("""
            select count(*)
            from pg_indexes
            where tablename = 'game_index'
              and indexname = ?
            """,
        Integer.class,
        indexName);
    return count != null && count > 0;
  }

  private void insertChunk(List<GameIndex> gameIndexes, long firstId, int start, int end) {
    StringBuilder sql = new StringBuilder("""
        insert into game_index (
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
        ) values
        """);
    List<Object> params = new ArrayList<>((end - start) * 11);
    for (int i = start; i < end; i++) {
      if (i > start) {
        sql.append(", ");
      }
      sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      GameIndex gameIndex = gameIndexes.get(i);
      params.add(firstId + i);
      params.add(gameIndex.getPawnStructure());
      params.add(gameIndex.getPieceConfiguration());
      params.add(gameIndex.getGameOffset());
      params.add(gameIndex.getMoveIndex());
      params.add(gameIndex.getWhitePlayer().getId());
      params.add(gameIndex.getBlackPlayer().getId());
      params.add(gameIndex.getWhiteElo());
      params.add(gameIndex.getBlackElo());
      params.add(gameIndex.getMaxElo());
      params.add(gameIndex.getTotalElo());
    }
    jdbcTemplate.update(sql.toString(), params.toArray());
  }

  private void syncSequenceWithTable() {
    jdbcTemplate.execute("""
        select setval('game_index_seq', (select coalesce(max(id), 0) + 1 from game_index), false)
        """);
  }

  private long reserveIds(int count) {
    // Reserve 'count' IDs by incrementing the sequence
    Long lastId = jdbcTemplate.queryForObject(
        "select nextval('game_index_seq')",
        Long.class);
    if (lastId == null) {
      throw new IllegalStateException("Could not get nextval from game_index_seq");
    }
    
    // The range we reserved is [lastId, lastId + count - 1]
    // We increment the sequence by count-1 more to reserve the whole block
    if (count > 1) {
      jdbcTemplate.execute("select setval('game_index_seq', " + (lastId + count - 1) + ", true)");
    }
    
    return lastId;
  }
}
