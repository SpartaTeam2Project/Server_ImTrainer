package com.trainergameserver.master.dto;

import java.util.List;

/** storage_monster.json 전체 구조 {"monsters": [...]} */
public record StorageMonsterTableData(List<StorageMonsterData> monsters) {
}
