package com.bakery.repository;

import com.bakery.model.UpiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UpiSettingsRepository extends JpaRepository<UpiSettings, Long> {
    Optional<UpiSettings> findFirstByOrderByIdAsc();
}
