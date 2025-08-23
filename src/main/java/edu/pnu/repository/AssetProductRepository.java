package edu.pnu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.AssetProduct;

public interface AssetProductRepository extends JpaRepository<AssetProduct, Long> {

}
