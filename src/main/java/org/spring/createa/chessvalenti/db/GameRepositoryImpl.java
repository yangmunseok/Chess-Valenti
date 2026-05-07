package org.spring.createa.chessvalenti.db;

import com.github.bhlangonijr.chesslib.game.Game;
import java.io.IOException;
import org.spring.createa.chessvalenti.util.pgn.FastPgnLoader;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

@Repository
@Scope("prototype")
public class GameRepositoryImpl implements GameRepository {

  FastPgnLoader loader;

  public GameRepositoryImpl() throws IOException {
    String pgnPath = "static/pgn/AJ-OTB-PGN-001.pgn";
    ClassPathResource resource = new ClassPathResource(pgnPath);
    String path = resource.getFile().getAbsolutePath();
    loader = new FastPgnLoader(path);
  }

  @Override
  public Game findGameByGameOffset(long offset) {
    try {
      return loader.loadGame(offset);
    } catch (IOException e) {
      return null;
    }
  }
}
