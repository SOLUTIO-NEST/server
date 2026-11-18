package com.solutio.api.domain.member.repository;

import com.solutio.api.domain.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM member WHERE student_id = :studentId AND is_deleted = true", nativeQuery = true)
    boolean existsWithdrawnByStudentId(@Param("studentId") String studentId);

    @Query(value = "SELECT * FROM member WHERE student_id = :studentId AND is_deleted = true", nativeQuery = true)
    Optional<Member> findWithdrawnByStudentId(@Param("studentId") String studentId);
}
