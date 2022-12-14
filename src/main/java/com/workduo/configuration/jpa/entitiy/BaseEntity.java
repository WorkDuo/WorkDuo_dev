package com.workduo.configuration.jpa.entitiy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    // 등록일, 등록일은 한번 등록하면 수정하지 못 하게 updatable false로 지정
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; // 생성 날짜

    // 수정일
    @LastModifiedDate
    private LocalDateTime updatedAt; // 수정 날짜
}
