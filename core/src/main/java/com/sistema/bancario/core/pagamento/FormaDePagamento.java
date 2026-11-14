package com.sistema.bancario.core.pagamento;

/**
 * Contrato Strategy para meios aceitos pelo PagamentoService.
 */
public interface FormaDePagamento {

    void validar(String destino, Double valor);

    void registrar(Long idContaOrigem, String tipoOperacao, String destino, Double valor);
}
