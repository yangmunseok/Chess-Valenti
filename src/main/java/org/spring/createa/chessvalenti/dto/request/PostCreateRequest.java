package org.spring.createa.chessvalenti.dto.request;

import org.spring.createa.chessvalenti.domain.PostType;

public record PostCreateRequest(String title, String content, String videoUrl, PostType postType) {

}
