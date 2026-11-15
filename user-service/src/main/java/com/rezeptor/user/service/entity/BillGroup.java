package com.rezeptor.user.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bill_groups")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class BillGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserModel createdBy;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "group")
    @Builder.Default
    private List<UserGroup> members = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void addMember(UserModel user) {
        members.add(new UserGroup(this, user));
    }
}
