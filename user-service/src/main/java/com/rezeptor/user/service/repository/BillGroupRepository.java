package com.rezeptor.user.service.repository;

import com.rezeptor.user.service.entity.BillGroupModel;
import com.rezeptor.user.service.entity.projection.UserGroupSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillGroupRepository extends JpaRepository<BillGroupModel, Long> {
    @Query(value = """
            select g.id as id,
            g.name as name,
            g.created_at as createdAt,
            count(all_m.user_id) as memberCount
            from bill_groups g
            join user_group me    on me.group_id = g.id and me.user_id = :userId
            join user_group all_m on all_m.group_id = g.id
            group by g.id, g.name, g.created_at
            order by g.created_at desc
            """, nativeQuery = true)
    List<UserGroupSummaryProjection> getUserGroupSummaryProjectionById(@Param("userId") Long userId);
}
