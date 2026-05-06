package org.spring.createa.chessvalenti.dto;

public class CustomString {

  String string;
  long offset;

  public CustomString(String string, long offset) {
    this.string = string;
    this.offset = offset;
  }

  public String getString() {
    return string;
  }

  public long getOffset() {
    return offset;
  }
}
