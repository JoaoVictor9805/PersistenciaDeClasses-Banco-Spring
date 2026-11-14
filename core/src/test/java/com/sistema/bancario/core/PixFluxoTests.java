package com.sistema.bancario.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sistema.bancario.core.boleto.Boleto;
import com.sistema.bancario.core.boleto.BoletoRepository;
import com.sistema.bancario.core.doc.Doc;
import com.sistema.bancario.core.doc.DocRepository;
import com.sistema.bancario.core.lotes.ItemLote;
import com.sistema.bancario.core.lotes.ItemLoteRepository;
import com.sistema.bancario.core.lotes.LoteService;
import com.sistema.bancario.core.pagamento.PagamentoService;
import com.sistema.bancario.core.pix.Pix;
import com.sistema.bancario.core.pix.PixRepository;
import com.sistema.bancario.core.saldo.Conta;
import com.sistema.bancario.core.saldo.ContaRepository;
import com.sistema.bancario.core.ted.Ted;
import com.sistema.bancario.core.ted.TedRepository;
import com.sistema.bancario.core.transferencia.TransferenciaService;

@SpringBootTest
@ActiveProfiles("test")
class PixFluxoTests {

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private PixRepository pixRepository;

    @Autowired
    private BoletoRepository boletoRepository;

    @Autowired
    private TedRepository tedRepository;

    @Autowired
    private DocRepository docRepository;

    @Autowired
    private ItemLoteRepository itemLoteRepository;

    @Autowired
    private LoteService loteService;

    private Conta contaOrigem;
    private Conta contaDestino;

    @BeforeEach
    void setup() {
        itemLoteRepository.deleteAll();
        boletoRepository.deleteAll();
        tedRepository.deleteAll();
        docRepository.deleteAll();
        pixRepository.deleteAll();
        contaRepository.deleteAll();

        contaOrigem = contaRepository.save(new Conta("0001-1", 1000.0, "origem@email.com"));
        contaDestino = contaRepository.save(new Conta("0002-2", 500.0, "12345678901"));
    }

    @Test
    void deveProcessarPagamentoPixComSucesso() {
        pagamentoService.processarPagamento(contaOrigem.getId(), 150.0, "PIX", "loja@email.com");

        Conta atualizada = contaRepository.findById(contaOrigem.getId()).orElseThrow();
        assertEquals(850.0, atualizada.getSaldoAtual());
        assertEquals(1, pixRepository.count());

        Pix registro = pixRepository.findAll().getFirst();
        assertEquals("PAGAMENTO", registro.getTipoOperacao());
        assertEquals(contaOrigem.getId(), registro.getIdContaOrigem());
        assertEquals("loja@email.com", registro.getChaveDestino());
        assertEquals(150.0, registro.getValor());
    }

    @Test
    void deveProcessarTransferenciaPixResolvendoContaDestinoPelaChave() {
        transferenciaService.processarTransferencia(contaOrigem.getId(), null, 200.0, "pix", "12345678901");

        Conta origemAtualizada = contaRepository.findById(contaOrigem.getId()).orElseThrow();
        Conta destinoAtualizada = contaRepository.findById(contaDestino.getId()).orElseThrow();

        assertEquals(800.0, origemAtualizada.getSaldoAtual());
        assertEquals(700.0, destinoAtualizada.getSaldoAtual());
        assertEquals(1, pixRepository.count());

        Pix registro = pixRepository.findAll().getFirst();
        assertEquals("TRANSFERENCIA", registro.getTipoOperacao());
        assertEquals(contaOrigem.getId(), registro.getIdContaOrigem());
        assertEquals("12345678901", registro.getChaveDestino());
    }

    @Test
    void devePermitirTransferenciaParaMesmaContaPelaChavePropria() {
        transferenciaService.processarTransferencia(contaOrigem.getId(), null, 100.0, "PIX", "origem@email.com");

        Conta origemAtualizada = contaRepository.findById(contaOrigem.getId()).orElseThrow();
        assertEquals(900.0 + 100.0, origemAtualizada.getSaldoAtual());
        assertEquals(1, pixRepository.count());
    }

    @Test
    void deveFalharQuandoChavePixForInvalidaSemAlterarSaldoNemSalvar() {
        assertThrows(IllegalArgumentException.class, () ->
                pagamentoService.processarPagamento(contaOrigem.getId(), 100.0, "PIX", "chave-invalida"));

        Conta origem = contaRepository.findById(contaOrigem.getId()).orElseThrow();
        assertEquals(1000.0, origem.getSaldoAtual());
        assertEquals(0, pixRepository.count());
    }

    @Test
    void deveFalharQuandoChavePixDestinoNaoPertencerANenhumaConta() {
        assertThrows(IllegalArgumentException.class, () ->
                transferenciaService.processarTransferencia(contaOrigem.getId(), null, 100.0, "PIX", "naoexiste@email.com"));

        Conta origem = contaRepository.findById(contaOrigem.getId()).orElseThrow();
        assertEquals(1000.0, origem.getSaldoAtual());
        assertEquals(0, pixRepository.count());
    }

    @Test
    void deveFalharQuandoIdDestinoInformadoNaoBaterComChavePix() {
        assertThrows(IllegalArgumentException.class, () ->
                transferenciaService.processarTransferencia(
                        contaOrigem.getId(), contaOrigem.getId(), 100.0, "PIX", "12345678901"));

        assertEquals(0, pixRepository.count());
    }

    @Test
    void deveFalharQuandoSaldoForInsuficienteSemSalvarPix() {
        assertThrows(RuntimeException.class, () ->
                pagamentoService.processarPagamento(contaOrigem.getId(), 5000.0, "PIX", "loja@email.com"));

        Conta origem = contaRepository.findById(contaOrigem.getId()).orElseThrow();
        assertEquals(1000.0, origem.getSaldoAtual());
        assertEquals(0, pixRepository.count());
    }

    @Test
    void deveFalharQuandoTipoDeOperacaoNaoForSuportado() {
        assertThrows(IllegalArgumentException.class, () ->
                pagamentoService.processarPagamento(contaOrigem.getId(), 50.0, "TED", "12345"));
        assertThrows(IllegalArgumentException.class, () ->
                transferenciaService.processarTransferencia(contaOrigem.getId(), contaDestino.getId(), 50.0, "BOLETO", "123"));
        assertThrows(IllegalArgumentException.class, () ->
                pagamentoService.processarPagamento(contaOrigem.getId(), 50.0, "CARTAO", "12345"));
    }

    @Test
    void deveFalharQuandoValorForZeroOuNegativo() {
        assertThrows(IllegalArgumentException.class, () ->
                pagamentoService.processarPagamento(contaOrigem.getId(), 0.0, "PIX", "loja@email.com"));
        assertThrows(IllegalArgumentException.class, () ->
                transferenciaService.processarTransferencia(contaOrigem.getId(), null, -10.0, "PIX", "12345678901"));
    }

    @Test
    void deveProcessarPagamentoBoletoEEnfileirarNoLote() {
        pagamentoService.processarPagamento(contaOrigem.getId(), 120.0, "BOLETO", "12345678901234");

        Conta atualizada = contaRepository.findById(contaOrigem.getId()).orElseThrow();
        assertEquals(880.0, atualizada.getSaldoAtual());
        assertEquals(1, boletoRepository.count());

        Boleto boleto = boletoRepository.findAll().getFirst();
        assertEquals(contaOrigem.getId(), boleto.getIdContaOrigem());
        assertEquals("12345678901234", boleto.getCodigoBarras());
        assertEquals(120.0, boleto.getValor());

        assertEquals(1, itemLoteRepository.count());
        ItemLote item = itemLoteRepository.findAll().getFirst();
        assertEquals("BOLETO", item.getTipoModalidade());
        assertEquals("PENDENTE", item.getStatus());
    }

    @Test
    void deveFalharQuandoCodigoDeBarrasDoBoletoForInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                pagamentoService.processarPagamento(contaOrigem.getId(), 100.0, "BOLETO", "123"));

        assertEquals(1000.0, contaRepository.findById(contaOrigem.getId()).orElseThrow().getSaldoAtual());
        assertEquals(0, boletoRepository.count());
        assertEquals(0, itemLoteRepository.count());
    }

    @Test
    void deveProcessarTransferenciaTedSemEnviarParaLote() {
        transferenciaService.processarTransferencia(contaOrigem.getId(), contaDestino.getId(), 300.0, "TED", null);

        assertEquals(700.0, contaRepository.findById(contaOrigem.getId()).orElseThrow().getSaldoAtual());
        assertEquals(800.0, contaRepository.findById(contaDestino.getId()).orElseThrow().getSaldoAtual());
        assertEquals(1, tedRepository.count());

        Ted ted = tedRepository.findAll().getFirst();
        assertEquals(String.valueOf(contaDestino.getId()), ted.getContaDestino());
        assertEquals(300.0, ted.getValor());
        assertEquals(0, itemLoteRepository.count());
    }

    @Test
    void deveProcessarTransferenciaDocEEnfileirarNoLote() {
        transferenciaService.processarTransferencia(contaOrigem.getId(), contaDestino.getId(), 250.0, "DOC", null);

        assertEquals(750.0, contaRepository.findById(contaOrigem.getId()).orElseThrow().getSaldoAtual());
        assertEquals(750.0, contaRepository.findById(contaDestino.getId()).orElseThrow().getSaldoAtual());
        assertEquals(1, docRepository.count());

        Doc doc = docRepository.findAll().getFirst();
        assertEquals(String.valueOf(contaDestino.getId()), doc.getContaDestino());
        assertEquals(250.0, doc.getValor());

        assertEquals(1, itemLoteRepository.count());
        ItemLote item = itemLoteRepository.findAll().getFirst();
        assertEquals("DOC", item.getTipoModalidade());
        assertEquals("PENDENTE", item.getStatus());
    }

    @Test
    void deveFalharQuandoDocExcederLimiteMaximo() {
        contaOrigem.setSaldoAtual(10000.0);
        contaRepository.save(contaOrigem);

        assertThrows(IllegalArgumentException.class, () ->
                transferenciaService.processarTransferencia(contaOrigem.getId(), contaDestino.getId(), 5000.0, "DOC", null));

        assertEquals(10000.0, contaRepository.findById(contaOrigem.getId()).orElseThrow().getSaldoAtual());
        assertEquals(0, docRepository.count());
        assertEquals(0, itemLoteRepository.count());
    }

    @Test
    void deveProcessarLotePendenteDeBoletoEDoc() {
        pagamentoService.processarPagamento(contaOrigem.getId(), 100.0, "BOLETO", "12345678901234");
        transferenciaService.processarTransferencia(contaOrigem.getId(), contaDestino.getId(), 200.0, "DOC", null);

        assertEquals(2, itemLoteRepository.findByStatus("PENDENTE").size());

        int processados = loteService.processarLotePendente();

        assertEquals(2, processados);
        assertEquals(0, itemLoteRepository.findByStatus("PENDENTE").size());
        assertEquals(2, itemLoteRepository.findByStatus("PROCESSADO").size());
    }
}
