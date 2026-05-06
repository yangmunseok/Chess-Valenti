package org.spring.createa.chessvalenti.db;

import org.jspecify.annotations.NullMarked;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Integer> {

  @NullMarked
  Page<Inquiry> findAll(Pageable pageable);

  Inquiry findInquiryById(int id);

  void deleteInquiriesById(int id);
}
