package com.sistema.bancario.core.doc;

import org.springframework.stereotype.Service;

import com.sistema.bancario.core.lotes.LoteService;
import com.sistema.bancario.core.transferencia.FormaDeTransferencia;

@Service("DOC")
public class DocService implements FormaDeTransferencia {

    private final DocRepository docRepository;
    private final LoteService loteService;

    public DocService(DocRepository docRepository, LoteService loteService) {
        this.docRepository = docRepository;
        this.loteService = loteService;
    }

    @Override
    public void validar(String destino, Double valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("Valor do DOC deve ser maior que zero");
        }
        if (valor >= 5000.0) {
            throw new IllegalArgumentException("DOC permite valores de até R$ 4.999,99");
        }
    }

    @Override
    public Long resolverContaDestino(Long idContaDestino, String chaveDestino) {
        if (idContaDestino == null) {
            throw new IllegalArgumentException("Conta de destino é obrigatória para DOC");
        }
        return idContaDestino;
    }

    @Override
    public void registrar(Long idContaOrigem, String tipoOperacao, String destino, Double valor) {
        docRepository.save(new Doc(idContaOrigem, destino, valor));
        loteService.adicionarAoLote("DOC", idContaOrigem, destino, valor);
    }
}
