package com.trainergameserver.master.dto;

import java.util.List;

/** training.json 전체 구조 {"trainings": [...]} */
public record TrainingTableData(List<TrainingData> trainings) {
}
