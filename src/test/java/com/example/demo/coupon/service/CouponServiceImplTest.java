package com.example.demo.coupon.service;

import com.example.demo.coupon.dto.CouponResponse;
import com.example.demo.coupon.dto.CouponSaveRequest;
import com.example.demo.coupon.entity.Coupon;
import com.example.demo.coupon.repository.CouponRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon sampleCoupon;
    private CouponSaveRequest saveRequest;

    @BeforeEach
    void setUp() {
        sampleCoupon = Coupon.builder()
                .id(1)
                .code("SUMMER20")
                .discountPercent(20)
                .expiredAt(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .build();

        saveRequest = CouponSaveRequest.builder()
                .code("SUMMER20")
                .discountPercent(20)
                .expiredAt(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .build();
    }

    @Test
    void getAllCoupons_ShouldReturnList() {
        when(couponRepository.findAll()).thenReturn(List.of(sampleCoupon));

        List<CouponResponse> result = couponService.getAllCoupons();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SUMMER20", result.get(0).getCode());
        verify(couponRepository, times(1)).findAll();
    }

    @Test
    void getCouponById_Found_ShouldReturnResponse() {
        when(couponRepository.findById(1)).thenReturn(Optional.of(sampleCoupon));

        CouponResponse result = couponService.getCouponById(1);

        assertNotNull(result);
        assertEquals("SUMMER20", result.getCode());
        verify(couponRepository, times(1)).findById(1);
    }

    @Test
    void getCouponById_NotFound_ShouldThrowException() {
        when(couponRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> couponService.getCouponById(1));
    }

    @Test
    void createCoupon_UniqueCode_ShouldSave() {
        when(couponRepository.findByCode("SUMMER20")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenReturn(sampleCoupon);

        CouponResponse result = couponService.createCoupon(saveRequest);

        assertNotNull(result);
        assertEquals("SUMMER20", result.getCode());
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }

    @Test
    void createCoupon_DuplicateCode_ShouldThrowException() {
        when(couponRepository.findByCode("SUMMER20")).thenReturn(Optional.of(sampleCoupon));

        assertThrows(RuntimeException.class, () -> couponService.createCoupon(saveRequest));
        verify(couponRepository, never()).save(any(Coupon.class));
    }
}
