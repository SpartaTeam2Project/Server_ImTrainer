package com.trainergameserver.master.repository;

import com.trainergameserver.master.entity.PlayerBase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerBaseRepository extends JpaRepository<PlayerBase, Integer> {
}
