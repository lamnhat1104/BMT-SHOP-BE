package com.example.demo.coupon.service;

import com.example.demo.coupon.dto.CouponResponse;
import com.example.demo.coupon.dto.CouponSaveRequest;
import java.util.List;

public interface CouponService {
    List<CouponResponse> getAllCoupons(Boolean showHidden);
    default List<CouponResponse> getAllCoupons() {
        return getAllCoupons(false);
    }
    CouponResponse getCouponById(Integer id);
    CouponResponse createCoupon(CouponSaveRequest request);
    CouponResponse updateCoupon(Integer id, CouponSaveRequest request);
    void deleteCoupon(Integer id);
}
