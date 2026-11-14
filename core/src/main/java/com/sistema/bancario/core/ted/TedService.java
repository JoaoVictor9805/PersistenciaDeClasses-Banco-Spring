package com.sistema.bancario.core.ted;

import org.springframework.stereotype.Service;

import com.sistema.bancario.core.transferencia.FormaDeTransferencia;

@Service("TED")
public class TedService implements FormaDeTransferencia {

    private final TedRepository tedRepository;

    public TedService(TedRepository tedRepository) {
        this.tedRepository = tedRepository;
    }

    @Override
    public void validar(String destino, Double valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("Valor da TED deve ser maior que zero");
        }
    }

    @Override
    public Long resolverContaDestino(Long idContaDestino, String chaveDestino) {
        if (idContaDestino == null) {
            throw new IllegalArgumentException("Conta de destino é obrigatória para TED");
        }
        return idContaDestino;
    }

    @Override
    public void registrar(Long idContaOrigem, String tipoOperacao, String destino, Double valor) {
        tedRepository.save(new Ted(idContaOrigem, destino, valor));
    }
}
