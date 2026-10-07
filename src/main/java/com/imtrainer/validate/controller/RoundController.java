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
@RequestMapping("/rounds/start")
@RequiredArgsConstructor
public class RoundController {

    private final RoundService roundService;

    @PostMapping
    public ResponseEntity<Void> startRound(@Valid @RequestBody RoundStartRequest request) {
        String trainerId = "T0000001";
        roundService.startRound(trainerId, request.roundStartTime());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}