package org.spring.createa.chessvalenti.dto;

import org.spring.createa.chessvalenti.domain.PostType;

public record AddPostBody(String title, String content, PostType postType) {

}
