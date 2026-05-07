package org.spring.createa.chessvalenti.dto.request;

import org.spring.createa.chessvalenti.domain.InquiryCategory;

public record InquiryCreateRequest(String title, String content, InquiryCategory category) {

}
