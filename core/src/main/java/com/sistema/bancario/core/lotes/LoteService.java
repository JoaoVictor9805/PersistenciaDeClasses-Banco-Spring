package com.sistema.bancario.core.lotes;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoteService {

    private final ItemLoteRepository itemLoteRepository;

    public LoteService(ItemLoteRepository itemLoteRepository) {
        this.itemLoteRepository = itemLoteRepository;
    }

    public void adicionarAoLote(String tipoModalidade, Long idContaOrigem, String destino, Double valor) {
        ItemLote item = new ItemLote(tipoModalidade, idContaOrigem, destino, valor);
        itemLoteRepository.save(item);
    }

    @Transactional
    public int processarLotePendente() {
        List<ItemLote> pendentes = itemLoteRepository.findByStatus("PENDENTE");
        for (ItemLote item : pendentes) {
            item.setStatus("PROCESSADO");
        }
        itemLoteRepository.saveAll(pendentes);
        return pendentes.size();
    }
}
