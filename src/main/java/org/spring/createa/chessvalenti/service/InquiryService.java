package org.spring.createa.chessvalenti.service;

import org.spring.createa.chessvalenti.db.InquiryRepository;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class InquiryService {

  InquiryRepository inquiryRepository;

  public InquiryService(InquiryRepository inquiryRepository) {
    this.inquiryRepository = inquiryRepository;
  }

  public Page<Inquiry> findAll(Pageable pageable) {
    Sort sort = Sort.by(Sort.Order.desc("createdAt"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);
    return inquiryRepository.findAll(pageRequest);
  }

  public Inquiry findInquiryById(int id) {
    return inquiryRepository.findInquiryById(id);
  }

  public void deleteInquiryById(int id) {
    inquiryRepository.deleteInquiriesById(id);
  }

  public void save(Inquiry inquiry) {
    inquiryRepository.save(inquiry);
  }
}
