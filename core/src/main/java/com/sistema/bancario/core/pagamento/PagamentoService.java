package com.sistema.bancario.core.pagamento;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.bancario.core.saldo.SaldoService;

@Service
public class PagamentoService {

    private final SaldoService saldoService;
    private final Map<String, FormaDePagamento> formasDePagamento;

    public PagamentoService(SaldoService saldoService, Map<String, FormaDePagamento> formasDePagamento) {
        this.saldoService = saldoService;
        this.formasDePagamento = formasDePagamento;
    }

    @Transactional
    public void processarPagamento(Long idConta, Double valor, String tipoPagamento, String chaveOuCodigo) {
        FormaDePagamento estrategia = buscarEstrategia(tipoPagamento);

        // 1. Validação específica do meio de pagamento antes de tocar no saldo
        estrategia.validar(chaveOuCodigo, valor);

        // 2. Efetivação financeira centralizada no SaldoService
        saldoService.debitarSaldo(idConta, valor);

        // 3. Persistência do comprovante após a movimentação bem-sucedida
        estrategia.registrar(idConta, "PAGAMENTO", chaveOuCodigo, valor);
    }

    private FormaDePagamento buscarEstrategia(String tipoPagamento) {
        if (tipoPagamento == null || tipoPagamento.isBlank()) {
            throw new IllegalArgumentException("Tipo de pagamento não informado");
        }
        FormaDePagamento estrategia = formasDePagamento.get(tipoPagamento.trim().toUpperCase());
        if (estrategia == null) {
            throw new IllegalArgumentException("Tipo de pagamento inválido ou não suportado: " + tipoPagamento);
        }
        return estrategia;
    }
}