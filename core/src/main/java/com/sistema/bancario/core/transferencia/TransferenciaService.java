package com.sistema.bancario.core.transferencia;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.bancario.core.saldo.SaldoService;

@Service
public class TransferenciaService {

    private final SaldoService saldoService;
    private final Map<String, FormaDeTransferencia> formasDeTransferencia;

    public TransferenciaService(SaldoService saldoService, Map<String, FormaDeTransferencia> formasDeTransferencia) {
        this.saldoService = saldoService;
        this.formasDeTransferencia = formasDeTransferencia;
    }

    @Transactional
    public void processarTransferencia(Long idContaOrigem, Long idContaDestino, Double valor, String tipoTransferencia, String chaveDestino) {
        FormaDeTransferencia estrategia = buscarEstrategia(tipoTransferencia);

        // 1. Validação específica e resolução do destino antes de tocar no saldo
        estrategia.validar(chaveDestino, valor);
        Long idDestinoFinal = estrategia.resolverContaDestino(idContaDestino, chaveDestino);

        // 2. Efetivação financeira de mão dupla
        saldoService.debitarSaldo(idContaOrigem, valor);
        saldoService.creditarSaldo(idDestinoFinal, valor);

        // 3. Persistência do comprovante após a movimentação bem-sucedida
        String destinoRegistrado = (chaveDestino != null && !chaveDestino.isBlank())
                ? chaveDestino
                : String.valueOf(idDestinoFinal);
        estrategia.registrar(idContaOrigem, "TRANSFERENCIA", destinoRegistrado, valor);
    }

    private FormaDeTransferencia buscarEstrategia(String tipoTransferencia) {
        if (tipoTransferencia == null || tipoTransferencia.isBlank()) {
            throw new IllegalArgumentException("Tipo de transferência não informado");
        }
        FormaDeTransferencia estrategia = formasDeTransferencia.get(tipoTransferencia.trim().toUpperCase());
        if (estrategia == null) {
            throw new IllegalArgumentException("Tipo de transferência inválido ou não suportado: " + tipoTransferencia);
        }
        return estrategia;
    }
}