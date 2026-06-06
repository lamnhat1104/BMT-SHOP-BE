package com.example.demo.payment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import java.net.URLEncoder;

@Component
@Getter
@Slf4j
public class VNPAYConfig {

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.version}")
    private String version;

    @Value("${vnpay.command}")
    private String command;

    public String getHashSecret() {
        return hashSecret != null ? hashSecret.trim() : null;
    }

    public String getTmnCode() {
        return tmnCode != null ? tmnCode.trim() : null;
    }

    public String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null || ipAdress.isEmpty()) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "127.0.0.1";
        }
        if (ipAdress == null || ipAdress.isEmpty() || ipAdress.contains(":") || "0:0:0:0:0:0:0:1".equals(ipAdress)) {
            ipAdress = "127.0.0.1";
        }
        return ipAdress;
    }

    public String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            return "";
        }
    }

    public String encodeParam(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    public String createPaymentUrl(String orderCode, double amount, String ipAddress) {
        String vnp_TxnRef = orderCode;
        String vnp_OrderInfo = "Thanh toan don hang " + orderCode;
        String vnp_OrderType = "other";
        
        long amountInCents = Math.round(amount * 100);
        
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        
        Map<String, String> vnp_Params = new TreeMap<>();

        vnp_Params.put("vnp_Version", version);
        vnp_Params.put("vnp_Command", command);
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInCents));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        boolean first = true;
        for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                    query.append('&');
                }

                // Chuỗi hash KHÔNG encode
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);

                // URL gửi sang VNPAY có encode (UTF-8, replace + with %20)
                query.append(encodeParam(fieldName));
                query.append('=');
                query.append(encodeParam(fieldValue));

                first = false;
            }
        }

        String secureHashInput = hashData.toString();

        String vnp_SecureHash =
                hmacSHA512(hashSecret, secureHashInput).toUpperCase();

        String paymentUrl =
                payUrl +
                "?" +
                query +
                "&vnp_SecureHash=" +
                vnp_SecureHash;

        log.info("========= VNPAY =========");
        log.info("HashData: {}", secureHashInput);
        log.info("SecureHash: {}", vnp_SecureHash);
        log.info("PaymentUrl: {}", paymentUrl);
        log.info("=========================");

        return paymentUrl;
    }
}
