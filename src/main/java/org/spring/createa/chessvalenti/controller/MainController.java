package org.spring.createa.chessvalenti.controller;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.dto.response.BoardResponse;
import org.spring.createa.chessvalenti.dto.game.GameInfo;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.dto.request.InquiryCreateRequest;
import org.spring.createa.chessvalenti.service.GameService;
import org.spring.createa.chessvalenti.service.InquiryService;
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
@Slf4j
@RequiredArgsConstructor
public class MainController {

  private final GameService gameService;
  private final PostService postService;
  private final InquiryService inquiryService;
  private final org.spring.createa.chessvalenti.db.InsightRepository insightRepository;

  @GetMapping("/")
  public String homepage(Model model) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() &&
        !(auth instanceof AnonymousAuthenticationToken)) {
      model.addAttribute("username", auth.getName());
      model.addAttribute("notices",
          postService.findAllByPostType(PageRequest.of(0, 5), PostType.NOTICE));
      model.addAttribute("faqs", postService.findFAQ());
      return "home";
    }
    return "index";
  }

  // Recommendation: Move to /analysis/old or merge with /analysis
  @GetMapping("/chess")
  public String chess(@RequestParam(defaultValue = "0") Long offset,
      @RequestParam(defaultValue = "0") Integer idx, Model model) {
    log.info("Accessing old chess view with offset: {}, idx: {}", offset, idx);
    Game game = gameService.getGameWithMoves(offset, idx);

    model.addAttribute("game", game);
    if (game != null) {
      model.addAttribute("pgn", game.toPgn(true, true));
      model.addAttribute("whiteTitle", game.getProperty().get("WhiteTitle"));
      model.addAttribute("blackTitle", game.getProperty().get("BlackTitle"));
    }

    return "oldIndex";
  }

  @GetMapping("/analysis")
  public String analysis(@RequestParam(required = false) Long offset,
      @RequestParam(defaultValue = "0") Integer idx, Model model, @AuthenticationPrincipal
      UserPrincipal userPrincipal) {
    log.info("Analysis page requested by user: {}, offset: {}", userPrincipal.getUsername(),
        offset);
    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("url", "/analysis");

    if (offset != null) {
      Game game = gameService.getGameWithMoves(offset, idx);
      if (game != null) {
        populateAnalysisModel(model, game);
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
      for (Move move : list) {
        board.doMove(move);
      }
    }

    List<String> legalMoves = ignoreLegalMove ? null :
        board.legalMoves().stream().map(Move::toString).toList();

    return new BoardResponse(board.getFen(), legalMoves, board.isKingAttacked(), board.isMated());
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
      @RequestParam(defaultValue = "false") boolean usePieceFilter) {
    log.info("Searching games by pawn structure: {}", fen);
    if (usePieceFilter) {
      return gameService.findGamesByPawnStructureAndPieceConfiguration(fen, whiteQueen,
              whiteRook, whiteBishop, whiteKnight, blackQueen, blackRook, blackBishop, blackKnight)
          .delayElements(Duration.ofMillis(1));
    }
    return gameService.findGamesByPawnStructure(fen).delayElements(Duration.ofMillis(1));
  }

  @GetMapping("/games/{id}")
  public String renderGame(@PathVariable Long id, @RequestParam(defaultValue = "0") Integer idx,
      Model model) {
    log.info("Rendering game by id: {}, idx: {}", id, idx);
    Game game = gameService.getGameWithMoves(id, idx);

    if (game != null) {
      populateAnalysisModel(model, game);
    }

    return "analysis";
  }

  private void populateAnalysisModel(Model model, Game game) {
    model.addAttribute("game", game);
    model.addAttribute("pgn", game.toPgn(true, true));
    model.addAttribute("whitePlayer", game.getWhitePlayer().getName());
    model.addAttribute("blackPlayer", game.getBlackPlayer().getName());
    model.addAttribute("whiteElo", game.getWhitePlayer().getElo());
    model.addAttribute("blackElo", game.getBlackPlayer().getElo());
    model.addAttribute("whiteTitle", game.getProperty().get("WhiteTitle"));
    model.addAttribute("blackTitle", game.getProperty().get("BlackTitle"));
    model.addAttribute("legalMove",
        game.getBoard().legalMoves().stream().map(Move::toString).toList());
    model.addAttribute("legalMoveSan",
        game.getBoard().legalMoves().stream().map(Move::getSan).toList());
    model.addAttribute("fen", game.getBoard().getFen());
  }

  @GetMapping("/insight")
  public String showInsight(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("url", "/insight");
    insightRepository.findByUser(userPrincipal.getUser()).ifPresent(insight -> {
      model.addAttribute("savedInsightData", insight.getData());
      model.addAttribute("savedLichessUsername", insight.getLichessUsername());
      model.addAttribute("savedPerfType", insight.getPerfType());
      model.addAttribute("savedSince", insight.getSince());
    });
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

  // Recommendation: Move to /api/support/inquiries
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/api/inquiries")
  public void createInquiry(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody InquiryCreateRequest body) {
    log.info("Creating inquiry by user: {}", userPrincipal.getUsername());
    inquiryService.save(
        new Inquiry(body.title(), body.content(), userPrincipal.getUser(), body.category())
    );
  }
}


