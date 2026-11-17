package com.rezeptor.settlement.svc.repository;

import com.rezeptor.settlement.svc.entity.SettlementModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<SettlementModel, Long> {
}
