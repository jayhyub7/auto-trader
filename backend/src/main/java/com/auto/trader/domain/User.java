package com.auto.trader.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users") // "user"는 예약어이므로 테이블명 변경 권장
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq_gen")
    @SequenceGenerator(name = "user_seq_gen", sequenceName = "user_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // 변경 가능성은 낮지만 unique 처리

    @Column(nullable = false)
    private String sub; // Google 고유 ID (변경 안 됨)

    private String name;         // 전체 이름
    private String givenName;    // 이름
    private String familyName;   // 성

    @Column(columnDefinition = "TEXT")
    private String picture;      // 프로필 사진 URL    

    private String provider; // "google" 등 OAuth 제공자 명

    @Column(length = 50)
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 기본값은 USER

} 