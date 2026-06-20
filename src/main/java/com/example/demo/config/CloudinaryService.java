package com.example.demo.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    
    // Directory where local uploads are stored
    private static final String LOCAL_UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    /**
     * Upload a MultipartFile to Cloudinary
     */
    public String uploadFile(MultipartFile file) throws IOException {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage());
            throw new IOException("Không thể tải ảnh lên đám mây", e);
        }
    }

    /**
     * Upload an existing image from a URL or local path to Cloudinary.
     * If the url is already a Cloudinary url, it returns it unchanged.
     * If it's a local /uploads/ URL, it reads the local file.
     * If it's an external HTTP URL, it passes the URL directly to Cloudinary.
     */
    public String uploadFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        // Already on Cloudinary or another secure hosting that we want to skip? 
        // We will skip if it already contains cloudinary.
        if (url.contains("res.cloudinary.com")) {
            return url;
        }

        try {
            if (url.startsWith("/uploads/")) {
                // Local file
                String filename = url.replace("/uploads/", "");
                File localFile = new File(LOCAL_UPLOAD_DIR + filename);
                if (localFile.exists()) {
                    Map uploadResult = cloudinary.uploader().upload(localFile, ObjectUtils.emptyMap());
                    return uploadResult.get("secure_url").toString();
                } else {
                    log.warn("Local file not found for migration: {}", localFile.getAbsolutePath());
                    return url; // Cannot migrate, return old url
                }
            } else if (url.startsWith("http://") || url.startsWith("https://")) {
                // External URL
                Map uploadResult = cloudinary.uploader().upload(url, ObjectUtils.emptyMap());
                return uploadResult.get("secure_url").toString();
            } else {
                // Some other format (maybe relative path, placeholder, etc.)
                log.warn("Unrecognized image URL format, skipping upload: {}", url);
                return url;
            }
        } catch (Exception e) {
            log.error("Failed to upload image from URL {}: {}", url, e.getMessage());
            return url; // Fallback to old url if failed
        }
    }
}
