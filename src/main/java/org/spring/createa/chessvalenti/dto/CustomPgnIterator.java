package org.spring.createa.chessvalenti.dto;

import java.util.Iterator;
import org.jspecify.annotations.NonNull;

public class CustomPgnIterator implements Iterable<CustomGame>, AutoCloseable {

  private final Iterator<CustomString> pgnLines;
  private CustomGame game;

  public CustomPgnIterator(String filename) throws Exception {
    this(new CustomLargeFile(filename));
  }

  public CustomPgnIterator(CustomLargeFile file) {
    this.pgnLines = file.iterator();
    loadNextGame();
  }

  /**
   * Constructs a new PGN iterator from an {@link Iterable} object that can iterate over the lines
   * of the PGN file.
   *
   * @param pgnLines an iterable over the PGN lines
   */
  public CustomPgnIterator(Iterable<CustomString> pgnLines) {

    this.pgnLines = pgnLines.iterator();
    loadNextGame();
  }

  /**
   * Constructs a new PGN iterator from another iterator over the lines of the PGN file.
   *
   * @param pgnLines an iterator over PGN lines
   */
  public CustomPgnIterator(Iterator<CustomString> pgnLines) {

    this.pgnLines = pgnLines;
    loadNextGame();
  }

  @Override
  public void close() throws Exception {
    if (pgnLines instanceof CustomLargeFile) {
      ((CustomLargeFile) (pgnLines)).close();
    }
  }

  @Override
  public @NonNull Iterator<CustomGame> iterator() {
    return new GameIterator();
  }

  private void loadNextGame() {
    game = CustomGameLoader.loadNextGame(pgnLines);
  }

  private class GameIterator implements Iterator<CustomGame> {

    public boolean hasNext() {
      if (game == null) {
        if (pgnLines.hasNext()) {
          loadNextGame();
        }
      }
      return game != null;
    }

    public CustomGame next() {

      CustomGame current = game;
      loadNextGame();
      return current;
    }

    public void remove() {
    }
  }
}
