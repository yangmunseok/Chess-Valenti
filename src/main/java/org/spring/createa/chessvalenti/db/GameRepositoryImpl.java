package org.spring.createa.chessvalenti.db;

import com.github.bhlangonijr.chesslib.game.Game;
import java.io.IOException;
import java.nio.file.Path;
import org.spring.createa.chessvalenti.util.pgn.FastPgnLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("prototype")
public class GameRepositoryImpl implements GameRepository {

  FastPgnLoader loader;

  public GameRepositoryImpl(@Value("${chess.pgn.path}") String pgnPath) throws IOException {
    Path path = Path.of(pgnPath);
    loader = new FastPgnLoader(path.toFile().getAbsolutePath());
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
