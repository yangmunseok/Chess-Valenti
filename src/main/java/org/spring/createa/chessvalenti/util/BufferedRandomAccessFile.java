package org.spring.createa.chessvalenti.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import lombok.Getter;

//Add buffering functionality when method readline() called from RandomAccessFile
public class BufferedRandomAccessFile {

  private int bufferSize;
  private int bufferLimit;
  private byte[] buffer;
  private int bufferOffset = 0;
  @Getter
  private long realFilePointer = 0;
  private long virtualFilePointer = 0;
  private boolean isFileEnd = false;
  private long lineOffset = 0;
  private RandomAccessFile raf;

  public BufferedRandomAccessFile(RandomAccessFile raf, int bufferSize) {
    this.raf = raf;
    this.bufferSize = bufferSize;
    this.buffer = new byte[this.bufferSize];
  }

  public BufferedRandomAccessFile(RandomAccessFile raf) {
    this.raf = raf;
    this.bufferSize = 8192;
    this.buffer = new byte[this.bufferSize];
  }

  private int findFromBuffer(byte input) {
    for (int i = bufferOffset; i < 8192; i++) {
      if (buffer[i] == input) {
        return i;
      }
    }
    return -1;
  }

  public String readLine() throws IOException {
    if (bufferLimit == -1) {
      return null;
    }

    int eol = findFromBuffer((byte) '\n');
    if (eol != -1 && eol <= bufferLimit) {
      String line = new String(buffer, bufferOffset, eol - bufferOffset, StandardCharsets.UTF_8);

      virtualFilePointer = realFilePointer - (bufferLimit - eol);
      bufferOffset = eol + 1;

      return line;
    }
    if (isFileEnd) {
      virtualFilePointer = realFilePointer;
      if (bufferOffset < bufferLimit) {
        return new String(buffer, bufferOffset, bufferLimit - bufferOffset, StandardCharsets.UTF_8);
      }
      return null;
    }
    if (realFilePointer > 0) {
      raf.seek(realFilePointer - (8192 - bufferOffset));
    }
    bufferLimit = raf.read(buffer);
    if (bufferLimit < bufferSize) {
      isFileEnd = true;
    }
    realFilePointer = raf.getFilePointer();
    bufferOffset = 0;
    return readLine();
  }

  public long getFilePointer() {
    return virtualFilePointer;
  }

  public void close() throws IOException {
    raf.close();
  }
}
