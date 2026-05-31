package com.example.demo.review.service;

import com.example.demo.review.dto.ReviewResponse;
import com.example.demo.review.entity.Review;
import com.example.demo.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review sampleReview;

    @BeforeEach
    void setUp() {
        sampleReview = Review.builder()
                .id(1)
                .productId(2)
                .userId(3)
                .rating(5)
                .comment("Excellent quality!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllReviews_ShouldReturnMappedResponses() {
        when(reviewRepository.findAll()).thenReturn(List.of(sampleReview));

        List<ReviewResponse> result = reviewService.getAllReviews();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Excellent quality!", result.get(0).getComment());
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    void deleteReview_Found_ShouldToggleIsActive() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> reviewService.deleteReview(1));

        assertFalse(sampleReview.getIsActive());
        verify(reviewRepository, times(1)).findById(1);
        verify(reviewRepository, times(1)).save(sampleReview);
    }

    @Test
    void deleteReview_NotFound_ShouldThrowException() {
        when(reviewRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> reviewService.deleteReview(1));
        verify(reviewRepository, never()).save(any(Review.class));
    }
}
