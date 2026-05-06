package org.spring.createa.chessvalenti.db;

import org.jspecify.annotations.NullMarked;
import org.spring.createa.chessvalenti.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

  @NullMarked
  Page<Payment> findAll(Pageable pageable);
}
