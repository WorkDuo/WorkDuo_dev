package com.workduo.sport.sport.entity;

import com.workduo.sport.sportcategory.entity.SportCategory;
import lombok.*;

import javax.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sport")
public class Sport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sport_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_category_id")
    private SportCategory sportCategory;

    private String name;

    @Lob
    private String emojiPath;
}
