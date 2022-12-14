package com.workduo.group.group.entity;

import com.workduo.area.siggarea.entity.SiggArea;
import com.workduo.configuration.jpa.entitiy.BaseEntity;
import com.workduo.error.group.exception.GroupException;
import com.workduo.error.group.type.GroupErrorCode;
import com.workduo.group.group.dto.UpdateGroup;
import com.workduo.group.group.type.GroupStatus;
import com.workduo.sport.sport.entity.Sport;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

import static com.workduo.error.group.type.GroupErrorCode.GROUP_LESS_THEN_PARTICIPANT;
import static com.workduo.group.group.type.GroupStatus.GROUP_STATUS_CANCEL;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "group_table")
public class Group extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    private String name;
    private Integer limitPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sgg")
    private SiggArea siggArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @Lob
    private String introduce;

    @Lob
    private String thumbnailPath;

    @Enumerated(EnumType.STRING)
    private GroupStatus groupStatus;
    private LocalDateTime deletedAt; // 삭제(탈퇴) 날짜

    public void updateGroupStatusCancel() {
        this.groupStatus = GROUP_STATUS_CANCEL;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateThumbnail(String filePath) {
        this.thumbnailPath = filePath;
    }

    public void groupUpdate(UpdateGroup.Request request, Integer countByGroup) {
        if (countByGroup > request.getLimitPerson()) {
            throw new GroupException(GROUP_LESS_THEN_PARTICIPANT);
        }

        this.introduce = request.getIntroduce();
        this.limitPerson = request.getLimitPerson();
    }
}
