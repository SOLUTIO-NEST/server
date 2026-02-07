package com.solutio.api.domain.blacklist.repository;

import com.solutio.api.domain.blacklist.domain.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

}
