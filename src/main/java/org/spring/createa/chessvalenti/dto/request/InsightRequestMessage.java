package org.spring.createa.chessvalenti.dto.request;

public record InsightRequestMessage(String username, String perfType, String since,
                                    Boolean cancel, Long id) {

}
