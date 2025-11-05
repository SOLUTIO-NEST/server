package com.solutio.api.domain.recruitment.domain;

import com.solutio.api.global.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Recruitment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    private String title;

    @NotNull(message = "시작 일시는 필수 값입니다.")
    private LocalDateTime startDateTime;

    @NotNull(message = "종료 일시는 필수 값입니다.")
    private LocalDateTime endDateTime;

}
