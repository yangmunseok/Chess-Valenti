package org.spring.createa.chessvalenti.service;

import java.time.LocalDateTime;
import org.spring.createa.chessvalenti.db.InquiryRepository;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  public Page<Inquiry> findAllByWriter(User writer, Pageable pageable) {
    Sort sort = Sort.by(Sort.Order.desc("createdAt"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);
    return inquiryRepository.findAllByWriter(writer, pageRequest);
  }

  @Transactional
  public void deleteInquiryById(int id) {
    inquiryRepository.deleteInquiriesById(id);
  }

  @Transactional
  public void answerInquiry(int id, String answer) {
    Inquiry inquiry = inquiryRepository.findInquiryById(id);
    inquiry.setAnswer(answer);
    inquiry.setAnsweredAt(LocalDateTime.now());
    inquiryRepository.save(inquiry);
  }

  public void save(Inquiry inquiry) {
    inquiryRepository.save(inquiry);
  }
}
