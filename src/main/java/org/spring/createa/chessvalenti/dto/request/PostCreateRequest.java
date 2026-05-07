package org.spring.createa.chessvalenti.dto.request;

import org.spring.createa.chessvalenti.domain.PostType;

public record PostCreateRequest(String title, String content, PostType postType) {

}
