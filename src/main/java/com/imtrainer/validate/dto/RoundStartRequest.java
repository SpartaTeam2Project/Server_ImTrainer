package com.imtrainer.validate.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RoundStartRequest(@NotNull LocalDateTime roundStartTime) { }
