package org.spring.createa.chessvalenti.dto.response;

import java.util.List;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;

public record AdminUserStatsResponse(
    Page<User> users,
    List<User> onlineUsers,
    int newUsersCnt,
    int diffNewUser,
    int onlineUserCnt,
    Double membershipRatio,
    int newSupporter,
    int diffNewSupporter
) {

}
