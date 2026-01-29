package com.solutio.api.domain.recruitment.domain;

import com.solutio.api.global.domain.BaseEntity;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE recruitment SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Recruitment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    private Recruitment(
        String title,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        this.title = title;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.isDeleted = false;
    }

    public static Recruitment create(
        String title,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        Recruitment recruitment = new Recruitment(title, startDateTime, endDateTime);

        recruitment.validateDateRange();

        return recruitment;
    }

    public void update(String title, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.title = title;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        validateDateRange();
    }

    private void validateDateRange() {
        if (startDateTime.isAfter(endDateTime)) {
            throw new GeneralException(Status.INVALID_DATE_RANGE);
        }
    }

    public void delete() {
        this.isDeleted=true;
    }
}
