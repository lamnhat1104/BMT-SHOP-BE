package com.example.demo.coupon.service;

import com.example.demo.coupon.dto.CouponResponse;
import com.example.demo.coupon.dto.CouponSaveRequest;
import com.example.demo.coupon.entity.Coupon;
import com.example.demo.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    public List<CouponResponse> getAllCoupons(Boolean showHidden) {
        return couponRepository.findAll().stream()
                .filter(c -> (showHidden != null && showHidden) || (c.getIsActive() == null || c.getIsActive()))
                .map(CouponResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public CouponResponse getCouponById(Integer id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mã khuyến mãi không tồn tại"));
        return CouponResponse.fromEntity(coupon);
    }

    @Override
    public CouponResponse createCoupon(CouponSaveRequest request) {
        if (couponRepository.findByCode(request.getCode().trim()).isPresent()) {
            throw new RuntimeException("Mã khuyến mãi đã tồn tại trên hệ thống");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .discountPercent(request.getDiscountPercent())
                .expiredAt(request.getExpiredAt())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        return CouponResponse.fromEntity(saved);
    }

    @Override
    public CouponResponse updateCoupon(Integer id, CouponSaveRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mã khuyến mãi không tồn tại"));

        Optional<Coupon> existingCoupon = couponRepository.findByCode(request.getCode().trim());
        if (existingCoupon.isPresent() && !existingCoupon.get().getId().equals(id)) {
            throw new RuntimeException("Mã khuyến mãi đã tồn tại trên hệ thống");
        }

        coupon.setCode(request.getCode().trim().toUpperCase());
        coupon.setDiscountPercent(request.getDiscountPercent());
        coupon.setExpiredAt(request.getExpiredAt());
        if (request.getIsActive() != null) {
            coupon.setIsActive(request.getIsActive());
        }

        Coupon saved = couponRepository.save(coupon);
        return CouponResponse.fromEntity(saved);
    }

    @Override
    public void deleteCoupon(Integer id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mã khuyến mãi không tồn tại"));
        coupon.setIsActive(coupon.getIsActive() == null || !coupon.getIsActive());
        couponRepository.save(coupon);
    }
}
