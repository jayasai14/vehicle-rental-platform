package com.vehiclerental.modules.location.repository;

import com.vehiclerental.modules.location.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByIsActiveTrue();
    Optional<City> findByNameIgnoreCase(String name);
}
