package org.spring.createa.chessvalenti.dto.body;

import org.spring.createa.chessvalenti.domain.InquiryCategory;

public record CreateInquiryBody(String title, String content, InquiryCategory category) {

}
