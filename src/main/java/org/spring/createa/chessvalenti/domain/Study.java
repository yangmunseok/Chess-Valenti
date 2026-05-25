package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@DiscriminatorValue("STUDY")
@Data
@EqualsAndHashCode(callSuper = true)
public class Study extends Post {

  String videoUrl;

  @Enumerated(EnumType.STRING)
  Difficulty difficulty;

  String introduction;

  String imageUrl;
}
