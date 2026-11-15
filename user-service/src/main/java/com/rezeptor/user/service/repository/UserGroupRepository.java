package com.rezeptor.user.service.repository;

import com.rezeptor.user.service.entity.UserGroupModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroupModel, Long> {
    @Query("""
            select ug from UserGroupModel ug
            join fetch ug.user u
            where ug.group.id = :groupId
            order by ug.joinedAt desc
            """)
    List<UserGroupModel> findMemberInGroupById(Long groupId);
}
