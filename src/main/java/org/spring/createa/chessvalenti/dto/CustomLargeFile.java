package org.spring.createa.chessvalenti.dto;

import java.io.RandomAccessFile;
import java.util.Iterator;
import org.jspecify.annotations.NonNull;
import org.spring.createa.chessvalenti.util.BufferedRandomAccessFile;

public class CustomLargeFile implements Iterable<CustomString>, AutoCloseable {

  long startTime = System.currentTimeMillis();
  long lastPrint = System.currentTimeMillis();

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
    reader = new BufferedRandomAccessFile(new RandomAccessFile(filepath, "r"));
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
      // 2초마다 출력
      if (System.currentTimeMillis() - lastPrint > 2000) {
        lastPrint = System.currentTimeMillis();

        long processedBytes = reader.getFilePointer();
        double progress = (double) processedBytes / (1024 * 1024);

        long elapsed = System.currentTimeMillis() - startTime;
        double speed = processedBytes / (elapsed / 1000.0);

        System.out.printf(
            "Loading | Progress: %.2f MB/s | Speed: %.2f MB/s\n",
            progress, speed / (1024 * 1024)
        );
      }
    } catch (Exception ex) {
      nextLine = null;
      throw new IllegalStateException("Error reading file", ex);
    }
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
