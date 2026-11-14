package com.sistema.bancario.core.pix;

import org.springframework.stereotype.Service;

import com.sistema.bancario.core.pagamento.FormaDePagamento;
import com.sistema.bancario.core.saldo.SaldoService;
import com.sistema.bancario.core.transferencia.FormaDeTransferencia;

@Service("PIX")
public class PixService implements FormaDePagamento, FormaDeTransferencia {

    private final PixRepository pixRepository;
    private final SaldoService saldoService;

    public PixService(PixRepository pixRepository, SaldoService saldoService) {
        this.pixRepository = pixRepository;
        this.saldoService = saldoService;
    }

    @Override
    public void validar(String chaveDestino, Double valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("Valor do Pix deve ser maior que zero");
        }
        if (chaveDestino == null || chaveDestino.isBlank()) {
            throw new IllegalArgumentException("Chave Pix inválida");
        }

        String chave = chaveDestino.trim();
        boolean ehEmail = chave.contains("@");
        boolean ehCpf = chave.length() == 11 && chave.chars().allMatch(Character::isDigit);

        if (!ehEmail && !ehCpf) {
            throw new IllegalArgumentException("Chave Pix inválida: informe um e-mail ou CPF com 11 dígitos");
        }
    }

    @Override
    public Long resolverContaDestino(Long idContaDestino, String chaveDestino) {
        Long idEncontrado = saldoService.buscarIdPorChavePix(chaveDestino.trim());
        if (idContaDestino != null && !idContaDestino.equals(idEncontrado)) {
            throw new IllegalArgumentException("A chave Pix informada não pertence à conta de destino");
        }
        return idEncontrado;
    }

    @Override
    public void registrar(Long idContaOrigem, String tipoOperacao, String chaveDestino, Double valor) {
        Pix pix = new Pix(idContaOrigem, tipoOperacao, chaveDestino.trim(), valor);
        pixRepository.save(pix);
    }
}
