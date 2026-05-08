package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.db.InquiryRepository;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class InquiryServiceTest {

  @Mock
  private InquiryRepository inquiryRepository;

  @InjectMocks
  private InquiryService inquiryService;

  @Test
  void findAll_ShouldReturnPage() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Inquiry> page = new PageImpl<>(List.of(new Inquiry()));
    when(inquiryRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Inquiry> result = inquiryService.findAll(pageable);

    assertFalse(result.isEmpty());
    verify(inquiryRepository).findAll(any(Pageable.class));
  }

  @Test
  void findInquiryById_ShouldReturnInquiry() {
    Inquiry inquiry = new Inquiry();
    when(inquiryRepository.findInquiryById(1)).thenReturn(inquiry);

    Inquiry result = inquiryService.findInquiryById(1);

    assertEquals(inquiry, result);
  }

  @Test
  void deleteInquiryById_ShouldCallRepositoryDelete() {
    inquiryService.deleteInquiryById(1);
    verify(inquiryRepository).deleteInquiriesById(1);
  }

  @Test
  void save_ShouldCallRepositorySave() {
    Inquiry inquiry = new Inquiry();
    inquiryService.save(inquiry);
    verify(inquiryRepository).save(inquiry);
  }
}
