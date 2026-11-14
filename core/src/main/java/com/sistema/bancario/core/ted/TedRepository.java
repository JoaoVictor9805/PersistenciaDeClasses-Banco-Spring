package com.sistema.bancario.core.ted;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TedRepository extends JpaRepository<Ted, Long> {
}
