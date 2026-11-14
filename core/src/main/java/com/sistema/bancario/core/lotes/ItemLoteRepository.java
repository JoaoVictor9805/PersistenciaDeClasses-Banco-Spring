package com.sistema.bancario.core.lotes;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemLoteRepository extends JpaRepository<ItemLote, Long> {

    List<ItemLote> findByStatus(String status);
}
