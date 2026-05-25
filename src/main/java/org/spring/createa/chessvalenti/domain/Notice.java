package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@DiscriminatorValue("NOTICE")
@Data
@EqualsAndHashCode(callSuper = true)
public class Notice extends Post {
}
