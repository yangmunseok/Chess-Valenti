package org.spring.createa.chessvalenti.dto;

import static com.github.bhlangonijr.chesslib.pgn.PgnProperty.UTF8_BOM;
import static com.github.bhlangonijr.chesslib.pgn.PgnProperty.isProperty;
import static com.github.bhlangonijr.chesslib.pgn.PgnProperty.parsePgnProperty;

import com.github.bhlangonijr.chesslib.game.Event;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.GameResult;
import com.github.bhlangonijr.chesslib.game.GenericPlayer;
import com.github.bhlangonijr.chesslib.game.Player;
import com.github.bhlangonijr.chesslib.game.PlayerType;
import com.github.bhlangonijr.chesslib.game.Round;
import com.github.bhlangonijr.chesslib.game.Termination;
import com.github.bhlangonijr.chesslib.game.TimeControl;
import com.github.bhlangonijr.chesslib.pgn.PgnException;
import com.github.bhlangonijr.chesslib.pgn.PgnProperty;
import com.github.bhlangonijr.chesslib.util.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class CustomGameLoader {

  private static boolean skip = false; // variable for skip freestyle or non standard start position;

  /**
   * Loads the next game of chess from an iterator over the lines of a Portable Game Notation (PGN)
   * file. The iteration ends when the game is fully loaded, hence the iterator is not consumed more
   * than necessary.
   *
   * @param iterator the iterator over the lines of a PGN file
   * @return the next game read from the iterator
   */
  public static CustomGame loadNextGame(Iterator<CustomString> iterator) {

    if (!iterator.hasNext()) {
      return null;
    }

    PgnTempContainer container = new PgnTempContainer();

    CustomString currentLine = iterator.next();
    container.game.setOffset(currentLine.getOffset());

    do {
      String line = currentLine.getString().trim();
      if (line.startsWith("ï»¿")) {
        line = line.substring(3);
      }
      if (line.startsWith(UTF8_BOM)) {
        line = line.substring(1);
      }
      try {
        if (isProperty(line)) {
          addProperty(line, container);
        } else if (StringUtils.isNotEmpty(line)) {
          if (!skip) {
            addMoveText(line, container);
          }
          if (isEndGame(line)) {
            skip = false;
            try {
              setMoveText(container.game, container.moveText);
            } catch (Exception e) {
              return null;
            }
            return container.initGame ? container.game : null;
          }
        }
      } catch (Exception e) { //TODO stricter exceptions
        String name = container.event.getName();
        int r = container.round.getNumber();
        throw new PgnException(String.format("Error parsing PGN[%d, %s]: ", r, name), e);
      }
      if (!iterator.hasNext()) {
        break;
      }
      currentLine = iterator.next();
    } while (true);

    return container.initGame ? container.game : null;
  }

  public static void skipGame(Iterator<CustomString> iterator) {

    CustomString currentLine;
    String line;
    do {
      currentLine = iterator.next();
      line = currentLine.getString().trim();
      if (line.startsWith("ï»¿")) {
        line = line.substring(3);
      }
      if (line.startsWith(UTF8_BOM)) {
        line = line.substring(1);
      }
    } while (!isEndGame(line));
  }

  public static Game loadOneGame(RandomAccessFile file) throws IOException {

    PgnTempContainer container = new PgnTempContainer();
    do {
      String current = file.readLine();
      if (current == null) {
        break;
      }
      String rawline = current.trim();
      if (rawline.startsWith("ï»¿")) {
        rawline = rawline.substring(3);
      }
      if (rawline.startsWith(UTF8_BOM)) {
        rawline = rawline.substring(1);
      }
      String line = new String(rawline.getBytes("ISO-8859-1"), "UTF-8");
      try {
        if (isProperty(line)) {
          addProperty(line, container);
        } else if (StringUtils.isNotEmpty(line)) {
          addMoveText(line, container);
          if (isEndGame(line)) {
            setMoveText(container.game, container.moveText);
            return container.initGame ? container.game : null;
          }
        }
      } catch (Exception e) { //TODO stricter exceptions
        System.out.println(e.getMessage());
        System.out.println(line);
        String name = container.event.getName();
        int r = container.round.getNumber();
        throw new PgnException(String.format("Error parsing PGN[%d, %s]: ", r, name), e);
      }
    } while (true);

    return container.initGame ? container.game : null;
  }

  public static String readLineUTF8(RandomAccessFile raf) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int b;
    while ((b = raf.read()) != -1) {
      if (b == '\n') {
        break;      // LF에서 종료
      }
      if (b != '\r') {
        buffer.write(b); // CR 무시
      }
    }
    if (buffer.size() == 0 && b == -1) {
      return null; // EOF
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }

  private static void addProperty(String line, PgnTempContainer container) throws Exception {
    PgnProperty property = parsePgnProperty(line);
    if (property == null) {
      return;
    }
    container.initGame = true;
    String tag = property.name.toLowerCase().trim();
    //begin
    switch (tag) {
      case "event":
        if (container.moveTextParsing && container.game.getHalfMoves().size() == 0) {
          setMoveText(container.game, container.moveText);
        }
        container.event.setName(property.value);
        container.event.setId(property.value);
        break;
      case "site":
        container.event.setSite(property.value);
        break;
      case "date":
        container.event.setStartDate(property.value);
        break;
      case "round":
        int r = 1;
        try {
          r = Integer.parseInt(property.value); //TODO isParseable
        } catch (Exception e1) {
        }
        r = Math.max(0, r);
        container.round.setNumber(r);
        if (!container.event.getRound().containsKey(r)) {
          container.event.getRound().put(r, container.round);
        }
        break;
      case "white": {
        if (container.round.getNumber() < 1) {
          container.round.setNumber(1); //TODO this is just to have the same behaviour as before...
        }

        container.game.setDate(container.event.getStartDate()); //TODO this should be done only once

        container.whitePlayer.setId(property.value);
        container.whitePlayer.setName(property.value);
        container.whitePlayer.setDescription(property.value);
        break;
      }
      case "black": {
        if (container.round.getNumber() < 1) {
          container.round.setNumber(1); //TODO this just to have the same behaviour as before...
        }

        container.game.setDate(container.event.getStartDate()); //TODO this should be done only once

        container.blackPlayer.setId(property.value);
        container.blackPlayer.setName(property.value);
        container.blackPlayer.setDescription(property.value);
        break;
      }
      case "result":
        container.game.setResult(GameResult.fromNotation(property.value));
        break;
      case "plycount":
        container.game.setPlyCount(property.value);
        break;
      case "termination":
        try {
          container.game.setTermination(Termination.fromValue(property.value.toUpperCase()));
        } catch (Exception e1) {
          container.game.setTermination(Termination.UNTERMINATED);
        }
        break;
      case "timecontrol":
        if (container.event.getTimeControl() == null) {
          try {
            container.event.setTimeControl(
                TimeControl.parseFromString(property.value.toUpperCase()));
          } catch (Exception e1) {
            //ignore errors in time control tag as it's not required by standards
          }
        }
        break;
      case "annotator":
        container.game.setAnnotator(property.value);
        break;
      case "fen":
        container.game.setFen(property.value);
        break;
      case "eco":
        container.game.setEco(property.value);
        break;
      case "opening":
        container.game.setOpening(property.value);
        break;
      case "variation":
        container.game.setVariation(property.value);
        break;
      case "whiteelo":
        try {
          container.whitePlayer.setElo(Integer.parseInt(property.value));
        } catch (NumberFormatException e) {

        }
        break;
      case "blackelo":
        try {
          container.blackPlayer.setElo(Integer.parseInt(property.value));
        } catch (NumberFormatException e) {

        }
        break;
      case "variant":
        if (property.value.equals("chess960")) {
          skip = true;
        }
      default:
        if (container.game.getProperty() == null) {
          container.game.setProperty(new HashMap<>());
        }
        container.game.getProperty().put(property.name, property.value);
        break;
    }
  }

  private static void addMoveText(String line, PgnTempContainer container) {
    container.initGame = true;
    container.moveText.append(line);
    container.moveText.append('\n');
    container.moveTextParsing = true;
  }

  private static boolean isEndGame(String line) {
    String stripped = stripBracketContent(line);
    return stripped.endsWith("1-0") || stripped.endsWith("0-1") || stripped.endsWith("1/2-1/2")
        || stripped.endsWith("*");
  }

  private static String stripBracketContent(String line) {
    int depth = 0;
    StringBuilder sb = new StringBuilder(line.length());
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}' && depth > 0) {
        depth--;
      } else if (depth == 0) {
        sb.append(c);
      }
    }
    return sb.toString().trim();
  }

  private static class PgnTempContainer {

    //TODO many of this stuff can be accessed through game

    final Event event;
    final Round round;
    final CustomGame game;
    Player whitePlayer;
    Player blackPlayer;
    final StringBuilder moveText;
    boolean moveTextParsing;
    boolean initGame;

    PgnTempContainer() {
      this.event = new Event();
      this.round = new Round(event);
      this.game = new CustomGame(UUID.randomUUID().toString(), round);
      this.round.getGame().add(this.game);
      this.whitePlayer = new GenericPlayer();
      this.whitePlayer.setType(PlayerType.HUMAN);
      this.game.setWhitePlayer(this.whitePlayer);
      this.blackPlayer = new GenericPlayer();
      this.blackPlayer.setType(PlayerType.HUMAN);
      this.game.setBlackPlayer(this.blackPlayer);
      this.moveText = new StringBuilder();
    }
  }

  private static void setMoveText(Game game, StringBuilder moveText) throws Exception {

    //clear game result
    StringUtil.replaceAll(moveText, "1-0", StringUtils.EMPTY);
    StringUtil.replaceAll(moveText, "0-1", StringUtils.EMPTY);
    StringUtil.replaceAll(moveText, "1/2-1/2", StringUtils.EMPTY);
    StringUtil.replaceAll(moveText, "*", StringUtils.EMPTY);

    game.setMoveText(moveText);
    game.loadMoveText(moveText);

    game.setPlyCount(String.valueOf(game.getHalfMoves().size()));

  }
}
