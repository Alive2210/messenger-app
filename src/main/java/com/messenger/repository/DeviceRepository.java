package com.messenger.repository;

import com.messenger.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByDeviceId(String deviceId);

    List<Device> findByUserId(UUID userId);

    List<Device> findByUserIdAndIsOnlineTrue(UUID userId);

    List<Device> findByIsOnlineTrue();

    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.isActive = true")
    List<Device> findActiveDevicesByUserId(@Param("userId") UUID userId);

    @Query("SELECT d FROM Device d WHERE d.lastHeartbeat < :threshold AND d.isOnline = true")
    List<Device> findStaleDevices(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.user.id = :userId AND d.isOnline = true")
    long countOnlineDevicesByUserId(@Param("userId") UUID userId);

    void deleteByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);
}
