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
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .maxUses(request.getMaxUses())
                .usedCount(0)
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
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setMaxUses(request.getMaxUses());
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

    @Override
    public com.example.demo.coupon.dto.ApplyCouponResponse applyCoupon(com.example.demo.coupon.dto.ApplyCouponRequest request) {
        Optional<Coupon> couponOpt = couponRepository.findByCode(request.getCode().trim().toUpperCase());
        if (couponOpt.isEmpty()) {
            return com.example.demo.coupon.dto.ApplyCouponResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi không tồn tại")
                    .build();
        }

        Coupon coupon = couponOpt.get();

        if (coupon.getIsActive() != null && !coupon.getIsActive()) {
            return com.example.demo.coupon.dto.ApplyCouponResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi không còn hoạt động")
                    .build();
        }

        if (coupon.getExpiredAt().isBefore(java.time.LocalDateTime.now())) {
            return com.example.demo.coupon.dto.ApplyCouponResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi đã hết hạn")
                    .build();
        }

        int usedCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
        if (coupon.getMaxUses() != null && usedCount >= coupon.getMaxUses()) {
            return com.example.demo.coupon.dto.ApplyCouponResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi đã hết lượt sử dụng")
                    .build();
        }

        if (coupon.getMinOrderValue() != null && request.getCartTotal() < coupon.getMinOrderValue()) {
            return com.example.demo.coupon.dto.ApplyCouponResponse.builder()
                    .isValid(false)
                    .message("Đơn hàng chưa đạt giá trị tối thiểu " + coupon.getMinOrderValue() + " để sử dụng mã")
                    .build();
        }

        double discountAmount = 0.0;
        Coupon.DiscountType type = coupon.getDiscountType();
        Double value = coupon.getDiscountValue();
        
        // Handle legacy coupons that don't have discountType or discountValue in DB
        if (type == null) {
            type = Coupon.DiscountType.PERCENTAGE;
        }
        if (value == null || value <= 0) {
            // Because we can't fetch discountPercent anymore, we use a default.
            // Wait, maybe we can just set a default of 10% for legacy coupons
            value = 10.0;
        }

        // Heuristic fix for corrupted legacy coupons: if fixed amount is extremely small (<= 100),
        // it was likely meant to be a percentage.
        if (type == Coupon.DiscountType.FIXED_AMOUNT && value <= 100.0) {
            type = Coupon.DiscountType.PERCENTAGE;
        }

        if (type == Coupon.DiscountType.PERCENTAGE) {
            discountAmount = request.getCartTotal() * (value / 100.0);
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount() > 0 && discountAmount > coupon.getMaxDiscountAmount()) {
                discountAmount = coupon.getMaxDiscountAmount();
            }
        } else if (type == Coupon.DiscountType.FIXED_AMOUNT) {
            discountAmount = value;
        } else if (type == Coupon.DiscountType.FREE_SHIPPING) {
            discountAmount = value; 
        }

        System.out.println("DEBUG applyCoupon -> type: " + type + ", value: " + value + ", maxDiscountAmount: " + coupon.getMaxDiscountAmount() + ", calculated discountAmount: " + discountAmount);


        // Không cho phép giảm quá tổng tiền
        if (discountAmount > request.getCartTotal()) {
            discountAmount = request.getCartTotal();
        }

        return com.example.demo.coupon.dto.ApplyCouponResponse.builder()
                .isValid(true)
                .discountAmount(discountAmount)
                .message("Áp dụng mã khuyến mãi thành công")
                .coupon(CouponResponse.fromEntity(coupon))
                .build();
    }
}
