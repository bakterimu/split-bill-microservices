package com.rezeptor.user.service.repository;

import com.rezeptor.user.service.entity.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByEmail(String email);
    @Query(
            value = """
                select u.id, u.name, u.email, u.created_at
                from users u
                join user_group ug on ug.user_id = u.id
                where ug.group_id = :groupId
            """, nativeQuery = true
    )
    List<UserModel> findByGroupId(@Param("groupId") Long groupId);
}
