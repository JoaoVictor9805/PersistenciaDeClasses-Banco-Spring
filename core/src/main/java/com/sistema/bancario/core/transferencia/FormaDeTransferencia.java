package com.sistema.bancario.core.transferencia;

/**
 * Contrato Strategy para meios aceitos pelo TransferenciaService.
 */
public interface FormaDeTransferencia {

    void validar(String destino, Double valor);

    Long resolverContaDestino(Long idContaDestino, String chaveDestino);

    void registrar(Long idContaOrigem, String tipoOperacao, String destino, Double valor);
}
