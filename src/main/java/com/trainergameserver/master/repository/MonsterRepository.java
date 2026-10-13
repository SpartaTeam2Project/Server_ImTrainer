package com.trainergameserver.master.repository;

import com.trainergameserver.master.entity.Monster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonsterRepository extends JpaRepository<Monster, Integer> {
}
