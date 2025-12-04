package com.ayno.aynobe.entity;

import com.ayno.aynobe.entity.enums.TagKind;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tag_tagName", columnNames = {"tagName"})
        })
public class Tag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagId;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String tagName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TagKind tagKind;
}
