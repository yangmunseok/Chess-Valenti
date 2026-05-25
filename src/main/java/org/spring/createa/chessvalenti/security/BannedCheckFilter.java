package org.spring.createa.chessvalenti.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class BannedCheckFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
      UserPrincipal user = (UserPrincipal) auth.getPrincipal();

      if (user.isBanned()) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}