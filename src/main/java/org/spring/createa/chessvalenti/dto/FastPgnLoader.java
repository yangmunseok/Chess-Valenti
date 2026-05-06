package org.spring.createa.chessvalenti.dto;

import com.github.bhlangonijr.chesslib.game.Game;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FastPgnLoader {

  private final RandomAccessFile pgnFile;

  public FastPgnLoader(String path) throws FileNotFoundException {
    pgnFile = new RandomAccessFile(path, "r");
  }


  public Game loadGame(long offset) throws IOException {
    try {
      pgnFile.seek(offset);
      return CustomGameLoader.loadOneGame(pgnFile);
    } catch (IOException e) {
      return null;
    }
  }
}
