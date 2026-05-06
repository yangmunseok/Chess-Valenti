package org.spring.createa.chessvalenti.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class TimeUtil {

  public static String timeAgo(LocalDateTime past) {
    LocalDateTime now = LocalDateTime.now();
    Duration duration = Duration.between(past, now);

    long seconds = duration.getSeconds();
    if (seconds < 60) {
      return seconds + "초 전";
    }
    long minutes = duration.toMinutes();
    if (minutes < 60) {
      return minutes + "분 전";
    }
    long hours = duration.toHours();
    if (hours < 24) {
      return hours + "시간 전";
    }
    long days = duration.toDays();
    return days + "일 전";
  }

  public String format(LocalDateTime dateTime, String pattern) {
    if (dateTime == null) {
      return "";
    }
    return dateTime.format(DateTimeFormatter.ofPattern(pattern));
  }

}
