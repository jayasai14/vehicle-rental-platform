package com.vehiclerental.modules.vehicle.repository;

import com.vehiclerental.modules.vehicle.entity.VehicleMaintenanceBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleMaintenanceBlockRepository extends JpaRepository<VehicleMaintenanceBlock, Long> {

    List<VehicleMaintenanceBlock> findByVehicleId(Long vehicleId);

    @Query("SELECT mb FROM VehicleMaintenanceBlock mb " +
           "WHERE mb.vehicle.id = :vehicleId " +
           "AND mb.startTime < :endTime AND mb.endTime > :startTime")
    List<VehicleMaintenanceBlock> findOverlappingBlocks(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
