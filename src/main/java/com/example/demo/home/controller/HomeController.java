package com.example.demo.home.controller;

import com.example.demo.home.dto.HomeResponse;
import com.example.demo.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ResponseEntity<HomeResponse> getHomeData() {
        return ResponseEntity.ok(homeService.getHomeData());
    }
}
