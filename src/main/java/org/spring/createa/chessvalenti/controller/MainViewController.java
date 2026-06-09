package org.spring.createa.chessvalenti.controller;

import com.github.bhlangonijr.chesslib.game.Game;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
@RequiredArgsConstructor
public class MainViewController {

    private final GameService gameService;
    private final PostService postService;
    private final InquiryService inquiryService;
    private final CommentService commentService;
    private final ChessBoardService chessBoardService;
    private final InsightRepository insightRepository;

    @GetMapping("/favicon.ico")
    @ResponseBody
    public void returnNoFavicon() {
    }

    @GetMapping("/")
    public String homepage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = "Guest";
        boolean isLoggedIn = false;
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            username = auth.getName();
            isLoggedIn = true;
        }

        model.addAttribute("username", username);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("notices", postService.findAllByPostType(PageRequest.of(0, 5), PostType.NOTICE));
        model.addAttribute("faqs", postService.findFAQ());
        return "home";
    }

    @GetMapping("/landing")
    public String landingPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken));
        model.addAttribute("isLoggedIn", isLoggedIn);
        return "landing";
    }

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
                           @RequestParam(defaultValue = "0") Integer idx, Model model,
                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String username = (userPrincipal != null) ? userPrincipal.getUsername() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute("isLoggedIn", userPrincipal != null);
        model.addAttribute("url", "/analysis");

        Game game = (offset != null) ? gameService.getGameWithMoves(offset, idx) : null;
        chessBoardService.populateAnalysisModel(model, game);
        return "analysis";
    }

    @GetMapping("/games/{id}")
    public String renderGame(@PathVariable Long id, @RequestParam(defaultValue = "0") Integer idx,
                             Model model, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Rendering game by id: {}, idx: {}", id, idx);
        Game game = gameService.getGameWithMoves(id, idx);
        chessBoardService.populateAnalysisModel(model, game);
        if (game != null) {
            model.addAttribute("idx", idx);
        }
        model.addAttribute("isLoggedIn", userPrincipal != null);
        model.addAttribute("username", (userPrincipal != null) ? userPrincipal.getUsername() : "Guest");
        return "analysis";
    }

    @GetMapping("/pawn-games")
    public String pawnGames(@RequestParam String fen,
                            @AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        String username = (userPrincipal != null) ? userPrincipal.getUsername() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute("isLoggedIn", userPrincipal != null);
        model.addAttribute("fen", fen);
        return "pawn-games";
    }

    @GetMapping("/insight")
    public String showInsight(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        String username = (userPrincipal != null) ? userPrincipal.getUsername() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute("isLoggedIn", userPrincipal != null);
        model.addAttribute("url", "/insight");
        if (userPrincipal != null) {
            insightRepository.findByUser(userPrincipal.getUser()).ifPresent(insight -> {
                model.addAttribute("savedInsightData", insight.getData());
                model.addAttribute("savedLichessUsername", insight.getLichessUsername());
                model.addAttribute("savedPerfType", insight.getPerfType());
                model.addAttribute("savedSince", insight.getSince());
            });
        }
        return "insight";
    }

    @GetMapping("/study")
    public String studyPage(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        String username = (userPrincipal != null) ? userPrincipal.getUsername() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute("isLoggedIn", userPrincipal != null);
        model.addAttribute("url", "/study");
        model.addAttribute("studies", postService.findAllByPostType(PageRequest.of(0, 20), PostType.STUDY).getContent());
        return "study";
    }

    @GetMapping("/support")
    public String supportPage() {
        return "redirect:/";
    }

    @GetMapping("/inquiries/{id}")
    public String inquiryDetailPage(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                    @PathVariable int id, Model model) {
        Inquiry inquiry = inquiryService.findInquiryById(id);
        if (inquiry == null) return "redirect:/inquiry";

        boolean isAdmin = userPrincipal != null && userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = userPrincipal != null && inquiry.getWriter().getUserId() == userPrincipal.getUser().getUserId();

        if (!isAdmin && !isOwner) return "redirect:/";

        model.addAttribute("post", inquiry);
        model.addAttribute("username", userPrincipal.getUsername());
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("url", "/inquiry");
        return "post-detail";
    }

    @GetMapping("/posts/{id}")
    public String postDetailPage(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                 @PathVariable int id, Model model) {
        String username = (userPrincipal != null) ? userPrincipal.getUsername() : "Guest";
        Post post = postService.findPostByPostId(id);
        model.addAttribute("post", post);
        model.addAttribute("username", username);
        model.addAttribute("isLoggedIn", userPrincipal != null);

        if (post.getType() == PostType.STUDY) model.addAttribute("url", "/study");
        else if (post.getType() == PostType.NOTICE || post.getType() == PostType.FAQ) model.addAttribute("url", "/");

        if (post.getType() != PostType.FAQ) {
            model.addAttribute("comments", commentService.findCommentsByPost(post));
            model.addAttribute("totalCommentCount", commentService.countCommentsByPost(post));
        }
        return "post-detail";
    }

    @GetMapping("/inquiry")
    public String inquiryPage(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        if (userPrincipal == null) return "redirect:/login";
        model.addAttribute("username", userPrincipal.getUsername());
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("url", "/inquiry");
        model.addAttribute("inquiries", inquiryService.findAllByWriter(userPrincipal.getUser(), PageRequest.of(0, 10)));
        return "inquiry";
    }
}
