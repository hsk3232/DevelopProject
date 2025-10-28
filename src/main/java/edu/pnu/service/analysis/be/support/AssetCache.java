package edu.pnu.service.analysis.be.support;

import edu.pnu.domain.AssetProduct;
import edu.pnu.repository.AssetProductRepository;
import edu.pnu.repository.AssetRouteRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class AssetCache {

        private final AssetRouteRepository assetRouteRepo;
        private final AssetProductRepository assetProductRepo;

        // v1: LogisticsFlowValidatorService 역할
        private Set<String> validRoutes;

        // v1: FindAnomaly Componet의 제품 정보 관련 역할
        private Set<String> knownProductCodes;
        private Set<String> knownCompanyCodes;
        private Set<String> knownProductNames;

        @PostConstruct
        @Transactional(readOnly = true)
        public void initialize() {
            // 1. 정상 경로 정보 캐싱
            validRoutes = assetRouteRepo.findAll().stream()
                    .map(route -> route.getFromLocationId().getLocationId()
                            + "->" + route.getToLocationId().getLocationId())
                    .collect(Collectors.toSet());

            // 2. 자산 제품 정보 캐싱
            Set<AssetProduct> allProducts = assetProductRepo.findAll().stream().collect(Collectors.toSet());
            knownProductCodes = allProducts.stream().map(AssetProduct::getEpcProduct).collect(Collectors.toSet());
            knownCompanyCodes = allProducts.stream().map(AssetProduct::getEpcCompany).collect(Collectors.toSet());
            knownProductNames = allProducts.stream().map(AssetProduct::getProductName).collect(Collectors.toSet());
        }
    }

