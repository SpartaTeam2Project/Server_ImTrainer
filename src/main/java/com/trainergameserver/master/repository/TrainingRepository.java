package com.trainergameserver.master.repository;

import com.trainergameserver.master.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingRepository extends JpaRepository<Training, Integer> {
}
