package org.spring.createa.chessvalenti.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.response.BoardResponse;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChessBoardService {

    /**
     * Calculates the board state based on FEN or SAN.
     */
    public BoardResponse getBoardState(String fen, String san, boolean ignoreLegalMove) {
        Board board = new Board();
        List<String> uciMoves = new ArrayList<>();
        if (fen != null) {
            board.loadFromFen(fen);
        } else if (san != null) {
            MoveList list = new MoveList();
            list.loadFromSan(san);
            for (Move move : list) {
                board.doMove(move);
                uciMoves.add(move.toString());
            }
        }

        List<String> legalMoves = ignoreLegalMove ? null :
                board.legalMoves().stream().map(Move::toString).toList();

        return new BoardResponse(board.getFen(), legalMoves, board.isKingAttacked(), board.isMated(), uciMoves);
    }

    /**
     * Populates the UI model with game analysis data.
     */
    public void populateAnalysisModel(Model model, Game game) {
        if (game == null) {
            Board board = new Board();
            model.addAttribute("game", new Game("", null));
            model.addAttribute("pgn", "");
            model.addAttribute("whitePlayer", "anonymous");
            model.addAttribute("blackPlayer", "anonymous");
            model.addAttribute("whiteElo", "9999");
            model.addAttribute("blackElo", "9999");
            model.addAttribute("legalMove", board.legalMoves().stream().map(Move::toString).toList());
            model.addAttribute("legalMoveSan", board.legalMoves().stream().map(Move::getSan).toList());
            model.addAttribute("fen", board.getFen());
            return;
        }

        model.addAttribute("game", game);
        model.addAttribute("pgn", game.toPgn(true, true));
        model.addAttribute("whitePlayer", game.getWhitePlayer().getName());
        model.addAttribute("blackPlayer", game.getBlackPlayer().getName());
        model.addAttribute("whiteElo", game.getWhitePlayer().getElo());
        model.addAttribute("blackElo", game.getBlackPlayer().getElo());
        model.addAttribute("whiteTitle", game.getProperty().get("WhiteTitle"));
        model.addAttribute("blackTitle", game.getProperty().get("BlackTitle"));
        model.addAttribute("legalMove", game.getBoard().legalMoves().stream().map(Move::toString).toList());
        model.addAttribute("legalMoveSan", game.getBoard().legalMoves().stream().map(Move::getSan).toList());
        model.addAttribute("fen", game.getBoard().getFen());
        model.addAttribute("uciMoves", game.getHalfMoves().stream().map(Move::toString).toList());
    }
}
