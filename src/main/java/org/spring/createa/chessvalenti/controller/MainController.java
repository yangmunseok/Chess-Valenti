package org.spring.createa.chessvalenti.controller;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.dto.BoardResponse;
import org.spring.createa.chessvalenti.dto.GameInfo;
import org.spring.createa.chessvalenti.dto.UserPrincipal;
import org.spring.createa.chessvalenti.dto.body.CreateInquiryBody;
import org.spring.createa.chessvalenti.service.GameService;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.spring.createa.chessvalenti.service.LichessService;
import org.spring.createa.chessvalenti.service.PostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Flux;

@Controller
public class MainController {

  GameService gameService;
  LichessService lichessService;
  PostService postService;
  InquiryService inquiryService;

  public MainController(GameService gameService, LichessService lichessService,
      PostService postService, InquiryService inquiryService) {
    this.gameService = gameService;
    this.lichessService = lichessService;
    this.postService = postService;
    this.inquiryService = inquiryService;
  }

  @GetMapping("/")
  public String homepage(Model model) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() &&
        !(auth instanceof AnonymousAuthenticationToken)) {
      model.addAttribute("username", auth.getName());
      PageRequest.of(0, 5);
      model.addAttribute("notices",
          postService.findAllByPostType(PageRequest.of(0, 5), PostType.NOTICE));
      model.addAttribute("faqs", postService.findFAQ());
      return "home";
    }
    return "index";
  }

  @GetMapping("/chess")
  public String chess(@RequestParam(required = false) Long offset,
      @RequestParam(required = false) Integer idx, Model model)
      throws IOException {
    if (offset == null) {
      offset = 0L;
    }
    if (idx == null) {
      idx = 0;
    }
    //Game game = testService.getGameByOffset(offset);
    Game game = gameService.findGameByOffset(offset);
    try {
      game.loadMoveText();
      game.setCurrentMoveList(game.getHalfMoves());
      game.setBoard(new Board());
      if (idx > 0) {
        game.gotoMove(game.getCurrentMoveList(), idx - 1);
      }
      System.out.println(game.getBoard());
      System.out.println(game.getMoveText());
      System.out.println(game.getBoard().legalMoves());
    } catch (Exception ex) {
      System.out.println(ex);
    }

    model.addAttribute("game", game);
    model.addAttribute("pgn", game.toPgn(true, true));
    model.addAttribute("whiteTitle", game.getProperty().get("WhiteTitle"));
    model.addAttribute("blackTitle", game.getProperty().get("BlackTitle"));

    return "oldIndex";
  }

  @GetMapping("/analysis")
  public String analysis(@RequestParam(required = false) Long offset,
      @RequestParam(required = false) Integer idx, Model model, @AuthenticationPrincipal
      UserPrincipal userPrincipal) throws IOException {
    Game game;

    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("url", "/analysis");
    if (idx == null) {
      idx = 0;
    }

    if (offset != null) {
      game = gameService.findGameByOffset(offset);
      try {
        game.loadMoveText();
        game.setCurrentMoveList(game.getHalfMoves());
        game.setBoard(new Board());
        if (idx > 0) {
          game.gotoMove(game.getCurrentMoveList(), idx - 1);
        }
        model.addAttribute("pgn", game.toPgn(true, true));
        model.addAttribute("whitePlayer", game.getWhitePlayer());
        model.addAttribute("blackPlayer", game.getBlackPlayer());
        model.addAttribute("whiteElo", game.getWhitePlayer().getElo());
        model.addAttribute("blackElo", game.getBlackPlayer().getElo());
        model.addAttribute("whiteTitle", game.getProperty().get("WhiteTitle"));
        model.addAttribute("blackTitle", game.getProperty().get("BlackTitle"));
        model.addAttribute("legalMove",
            game.getBoard().legalMoves().stream().map(Move::toString).toList());
        model.addAttribute("legalMoveSan",
            game.getBoard().legalMoves().stream().map(Move::getSan).toList());
        model.addAttribute("fen", game.getBoard().getFen());
      } catch (Exception ex) {
        System.out.println(ex);
      }
    } else {
      Board board = new Board();
      model.addAttribute("game", new Game("", null));
      model.addAttribute("pgn", "");
      model.addAttribute("whitePlayer", "anonymous");
      model.addAttribute("blackPlayer", "anonymous");
      model.addAttribute("whiteElo", "9999");
      model.addAttribute("blackElo", "9999");
      model.addAttribute("legalMove",
          board.legalMoves().stream().map(Move::toString).toList());
      model.addAttribute("legalMoveSan",
          board.legalMoves().stream().map(Move::getSan).toList());
      model.addAttribute("fen", board.getFen());
    }
    return "analysis";
  }

  @ResponseBody
  @GetMapping("/board")
  public BoardResponse board(@RequestParam(required = false) String fen,
      @RequestParam(required = false) String san,
      @RequestParam(required = false) boolean ignoreLegalMove) {

    Board board = new Board();
    if (fen != null) {
      board.loadFromFen(fen);
    } else if (san != null) {
      MoveList list = new MoveList();
      list.loadFromSan(san);
      String[] sanMoves = list.toSanArray();

      for (Move move : list) {
        board.doMove(move);
      }
    }
    if (ignoreLegalMove) {
      return new BoardResponse(board.getFen(), null, false, board.isMated());
    }

    return new BoardResponse(board.getFen(),
        board.legalMoves().stream().map(Move::toString).toList(), board.isKingAttacked(),
        board.isMated());
  }

  @ResponseBody
  @GetMapping(value = "/api/games", produces = MediaType.APPLICATION_NDJSON_VALUE)
  public Flux<GameInfo> searchGamesByPawnStructure(@RequestParam String fen,
      @RequestParam(required = false) Integer whiteQueen,
      @RequestParam(required = false) Integer whiteRook,
      @RequestParam(required = false) Integer whiteBishop,
      @RequestParam(required = false) Integer whiteKnight,
      @RequestParam(required = false) Integer blackQueen,
      @RequestParam(required = false) Integer blackRook,
      @RequestParam(required = false) Integer blackBishop,
      @RequestParam(required = false) Integer blackKnight,
      @RequestParam(defaultValue = "false") boolean usePieceFilter, HttpServletResponse response) {
    if (usePieceFilter) {
      return gameService.findGamesByPawnStructureAndPieceConfiguration(fen, whiteQueen,
          whiteRook,
          whiteBishop,
          whiteKnight,
          blackQueen,
          blackRook,
          blackBishop,
          blackKnight).delayElements(Duration.ofMillis(1));
    }
    return gameService.findGamesByPawnStructure(fen).delayElements(Duration.ofMillis(1));
  }

  @GetMapping("/games/{id}")
  public String renderGame(@PathVariable Long id, @RequestParam(required = false) Integer idx,
      Model model) {
    if (idx == null) {
      idx = 0;
    }
    Game game = gameService.findGameByOffset(id);
    try {
      game.loadMoveText();
      game.setCurrentMoveList(game.getHalfMoves());
      game.setBoard(new Board());
      if (idx > 0) {
        game.gotoMove(game.getCurrentMoveList(), idx - 1);
      }
      System.out.println(game.getBoard());
      System.out.println(game.getMoveText());
    } catch (Exception ex) {
      System.out.println(ex);
      System.out.println("problem");
      System.out.println(game.toPgn(true, true));
      System.out.println("problem");
    }

    model.addAttribute("game", game);
    model.addAttribute("pgn", game.toPgn(true, true));
    model.addAttribute("whitePlayer", game.getWhitePlayer().getName());
    model.addAttribute("blackPlayer", game.getBlackPlayer().getName());
    model.addAttribute("whiteElo", game.getWhitePlayer().getElo());
    model.addAttribute("blackElo", game.getBlackPlayer().getElo());
    System.out.println(game.getProperty());

    model.addAttribute("legalMove",
        game.getBoard().legalMoves().stream().map(Move::toString).toList());
    model.addAttribute("legalMoveSan",
        game.getBoard().legalMoves().stream().map(Move::getSan).toList());
    model.addAttribute("fen", game.getBoard().getFen());
    return "analysis";
  }

  @GetMapping("/insight")
  public String showInsight(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("url", "/insight");
    return "insight";
  }

  @GetMapping("/support")
  public String supportPage(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("url", "/support");
    model.addAttribute("email", userPrincipal.getUser().getEmail());
    return "support";
  }

  @GetMapping("/posts/{id}")
  public String postDetailPage(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable int id, Model model) {
    model.addAttribute("post", postService.findPostByPostId(id));
    model.addAttribute("username", userPrincipal.getUsername());
    return "post-detail";
  }

  @GetMapping("/inquiry")
  public String inquiryPage(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("url", "/inquiry");
    return "inquiry";
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/api/inquiries")
  public void createInquiry(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody CreateInquiryBody body) {
    inquiryService.save(
        new Inquiry(body.title(), body.content(), userPrincipal.getUser(), body.category())
    );
  }
}


