package com.agv.AGVBackend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    // The generic types are: <Entity type, ID type>
    // No need to implement any methods - JPA will provide basic CRUD operations
}

