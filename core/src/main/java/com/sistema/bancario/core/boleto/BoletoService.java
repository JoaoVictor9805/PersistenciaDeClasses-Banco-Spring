package com.sistema.bancario.core.boleto;

import org.springframework.stereotype.Service;

import com.sistema.bancario.core.lotes.LoteService;
import com.sistema.bancario.core.pagamento.FormaDePagamento;

@Service("BOLETO")
public class BoletoService implements FormaDePagamento {

    private final BoletoRepository boletoRepository;
    private final LoteService loteService;

    public BoletoService(BoletoRepository boletoRepository, LoteService loteService) {
        this.boletoRepository = boletoRepository;
        this.loteService = loteService;
    }

    @Override
    public void validar(String codigoBarras, Double valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("Valor do boleto deve ser maior que zero");
        }
        if (codigoBarras == null || codigoBarras.isBlank()) {
            throw new IllegalArgumentException("Código de barras inválido");
        }

        String codigo = codigoBarras.trim();
        if (codigo.length() < 10 || !codigo.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Código de barras inválido: informe ao menos 10 dígitos numéricos");
        }
    }

    @Override
    public void registrar(Long idContaOrigem, String tipoOperacao, String codigoBarras, Double valor) {
        String codigo = codigoBarras.trim();
        boletoRepository.save(new Boleto(idContaOrigem, codigo, valor));
        loteService.adicionarAoLote("BOLETO", idContaOrigem, codigo, valor);
    }
}
