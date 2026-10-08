package com.imtrainer.round.dto;

import jakarta.validation.constraints.NotBlank;

public record RoundStartRequest(@NotBlank String trainerId) { }
