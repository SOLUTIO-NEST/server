package com.solutio.api.domain.applicant.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Embeddable
@NoArgsConstructor
public class ApplicantId {
    @EqualsAndHashCode.Include
    @Column(nullable = false, length = 9, unique = true, updatable = false)
    private String studentId;

    @EqualsAndHashCode.Include
    private Long recruitmentId;
}
