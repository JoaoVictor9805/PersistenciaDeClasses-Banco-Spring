package com.sistema.bancario.core.saldo; // Pacote ajustado

import org.springframework.stereotype.Service;

@Service
public class SaldoService {

    private final ContaRepository contaRepository;

    public SaldoService(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    // Método 1: Usado por Pagamentos e Transferências (Origem)
    public void debitarSaldo(Long idConta, Double valor) {
        validarValor(valor);

        Conta conta = contaRepository.findById(idConta)
                .orElseThrow(() -> new RuntimeException("Conta de origem não encontrada"));

        if (conta.getSaldoAtual() < valor) {
            throw new RuntimeException("Saldo insuficiente para a operação");
        }

        conta.setSaldoAtual(conta.getSaldoAtual() - valor);
        contaRepository.save(conta);
    }

    // Método 2: Usado APENAS por Transferências (Destino)
    public void creditarSaldo(Long idConta, Double valor) {
        validarValor(valor);

        Conta conta = contaRepository.findById(idConta)
                .orElseThrow(() -> new RuntimeException("Conta de destino não encontrada"));

        conta.setSaldoAtual(conta.getSaldoAtual() + valor);
        contaRepository.save(conta);
    }

    public Long buscarIdPorChavePix(String chavePix) {
        if (chavePix == null || chavePix.isBlank()) {
            throw new IllegalArgumentException("Chave Pix não pode ser vazia");
        }

        return contaRepository.findByChavePix(chavePix)
                .map(Conta::getId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada para a chave Pix informada"));
    }

    private void validarValor(Double valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("Valor da operação deve ser maior que zero");
        }
    }
}