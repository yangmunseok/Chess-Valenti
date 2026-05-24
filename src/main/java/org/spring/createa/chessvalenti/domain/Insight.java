package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spring.createa.chessvalenti.dto.game.GameResults;

@Entity
@Data
@NoArgsConstructor
public class Insight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "user_id")
  private User user;

  private String lichessUsername;
  private String perfType;
  private Long since;

  @Convert(converter = org.spring.createa.chessvalenti.util.InsightDataConverter.class)
  @Column(columnDefinition = "LONGTEXT")
  private Map<String, GameResults> data;

  private LocalDateTime createdAt = LocalDateTime.now();

  public Insight(User user, String lichessUsername, String perfType, Long since,
      Map<String, GameResults> data) {
    this.user = user;
    this.lichessUsername = lichessUsername;
    this.perfType = perfType;
    this.since = since;
    this.data = data;
    this.createdAt = LocalDateTime.now();
  }
}
