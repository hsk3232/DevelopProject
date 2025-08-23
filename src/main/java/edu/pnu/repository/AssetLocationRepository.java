package edu.pnu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.AssetLocation;

public interface AssetLocationRepository extends JpaRepository<AssetLocation, Long> {

}
