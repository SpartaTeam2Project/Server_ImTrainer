package com.imtrainer.validate.controller;

import com.imtrainer.validate.dto.RoundStartRequest;
import com.imtrainer.validate.service.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rounds")
@RequiredArgsConstructor
public class RoundController {

    private final RoundService roundService;

    @PostMapping("/start")
    public ResponseEntity<Void> startRound() {
        String trainerId = "T0000001";
        roundService.startRound(trainerId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}