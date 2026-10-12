package com.trainergameserver.repository;

import com.trainergameserver.entity.Monster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonsterRepository extends JpaRepository<Monster, Integer> {
}
