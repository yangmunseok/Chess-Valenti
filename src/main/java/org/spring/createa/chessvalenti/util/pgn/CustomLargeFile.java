package org.spring.createa.chessvalenti.util.pgn;

import java.io.RandomAccessFile;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.spring.createa.chessvalenti.util.BufferedRandomAccessFile;

@Slf4j
public class CustomLargeFile implements Iterable<CustomString>, AutoCloseable {

  long startTime = System.currentTimeMillis();
  long totalBytes;
  int lastLoggedProgress = 0;

  @Override
  public void close() throws Exception {
    try {
      reader.close();
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  @Override
  public @NonNull Iterator<CustomString> iterator() {
    return new FileIterator();
  }

  public CustomLargeFile(String filepath) throws Exception {
    RandomAccessFile file = new RandomAccessFile(filepath, "r");
    totalBytes = file.length();
    reader = new BufferedRandomAccessFile(file, 64 * 1024);
    readNextLine();
  }

  private void readNextLine() {
    try {
      long offset = reader.getFilePointer();
      String newLine = reader.readLine();
      if (newLine == null) {
        nextLine = null;
        return;
      }
      nextLine = new CustomString(newLine, offset);

      long processedBytes = reader.getFilePointer();
      int progress = calculateProgress(processedBytes);
      if (progress > lastLoggedProgress) {
        lastLoggedProgress = progress;

        long elapsed = System.currentTimeMillis() - startTime;
        double speed = processedBytes / (elapsed / 1000.0);

        log.info(
            String.format(
                "Loading | Progress: %d%% | Speed: %.2f MB/s",
                progress,
                speed / (1024.0 * 1024.0)
            )
        );
      }
    } catch (Exception ex) {
      nextLine = null;
      throw new IllegalStateException("Error reading file", ex);
    }
  }

  private int calculateProgress(long processedBytes) {
    if (totalBytes <= 0) {
      return 100;
    }
    return (int) Math.min(100, processedBytes * 100 / totalBytes);
  }

  private class FileIterator implements Iterator<CustomString> {

    @Override
    public boolean hasNext() {
      return nextLine != null;
    }

    @Override
    public CustomString next() {
      CustomString currentLine = nextLine;
      readNextLine();
      return currentLine;
    }
  }

  BufferedRandomAccessFile reader;
  CustomString nextLine;
}
