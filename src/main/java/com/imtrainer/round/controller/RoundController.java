package com.imtrainer.round.controller;

import com.imtrainer.round.dto.RoundEndRequest;
import com.imtrainer.round.dto.RoundStartRequest;
import com.imtrainer.round.service.RoundService;
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
    public ResponseEntity<Void> startRound(@Valid @RequestBody RoundStartRequest request) {
        roundService.startRound(request.trainerId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/end")
    public ResponseEntity<Void> endRound(@Valid @RequestBody RoundEndRequest request) {
        roundService.endRound(request);
        return ResponseEntity.ok().build();
    }
}