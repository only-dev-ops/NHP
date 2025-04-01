package com.nhp.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nhp.model.MonitoredPrefix;

public interface MonitoredPrefixesRepo extends JpaRepository<MonitoredPrefix, Long> {

    @Query("SELECT m.prefix FROM MonitoredPrefix m")
    List<String> findAllPrefixes();
}
