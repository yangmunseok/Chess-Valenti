package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
public class ChessPlayer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  long id;

  @Column(unique = true)
  String name;

  @Transient
  int rating;

  public ChessPlayer() {

  }

  public ChessPlayer(String name) {
    this.name = name;
  }

  public ChessPlayer(String name, int rating) {
    this.name = name;
    this.rating = rating;
  }

  @Override
  public String toString() {
    return id + ",\"" + name.replace("\"", "\"\"")
        + "\"," + rating;
  }
}
