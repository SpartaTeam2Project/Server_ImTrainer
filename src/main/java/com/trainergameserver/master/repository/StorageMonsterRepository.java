package com.trainergameserver.master.repository;

import com.trainergameserver.master.entity.StorageMonster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageMonsterRepository extends JpaRepository<StorageMonster, Integer> {
}
