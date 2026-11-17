package com.rezeptor.settlement.svc.repository;

import com.rezeptor.settlement.svc.entity.SettlementModel;
import com.rezeptor.settlement.svc.entity.projection.SettledBalanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<SettlementModel, Long> {

    @Query(value = """
            select t.user_id    as userId,
                   sum(t.delta) as netSettled
            from (
                select from_user_id as user_id, amount as delta
                from settlements
                where group_id = :groupId
                union all
                select to_user_id as user_id, -amount as delta
                from settlements
                where group_id = :groupId
            ) t
            group by t.user_id
            """, nativeQuery = true)
    List<SettledBalanceProjection> findNetSettledByGroupId(@Param("groupId") Long groupId);
}
