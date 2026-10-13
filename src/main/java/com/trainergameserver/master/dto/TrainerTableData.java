package com.trainergameserver.master.dto;

import java.util.List;

/** trainer.json 전체 구조 {"trainers": [...]} */
public record TrainerTableData(List<TrainerData> trainers) {
}
