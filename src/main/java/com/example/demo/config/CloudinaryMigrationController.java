package com.example.demo.config;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class CloudinaryMigrationController {

    private final CloudinaryMigrationService migrationService;

    @PostMapping("/migrate-images")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> migrateImages() {
        try {
            String result = migrationService.migrateAllImages();
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Lỗi trong quá trình migrate: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // TEMPORARY: Public endpoint to trigger migration without auth
    @PostMapping("/public/migrate-images")
    public ResponseEntity<?> migrateImagesPublic() {
        try {
            String result = migrationService.migrateAllImages();
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Lỗi trong quá trình migrate: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
