package com.rezeptor.user.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_group", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Setter
@Getter
@NoArgsConstructor
public class UserGroupModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "group_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private BillGroupModel group;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private UserModel user;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    public UserGroupModel(BillGroupModel group, UserModel user) {
        this.group = group;
        this.user = user;
    }
}
