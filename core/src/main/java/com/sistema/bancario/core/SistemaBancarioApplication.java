package com.sistema.bancario.core;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.sistema.bancario.core.boleto.BoletoRepository;
import com.sistema.bancario.core.doc.DocRepository;
import com.sistema.bancario.core.lotes.ItemLoteRepository;
import com.sistema.bancario.core.lotes.LoteService;
import com.sistema.bancario.core.pagamento.PagamentoService;
import com.sistema.bancario.core.pix.PixRepository;
import com.sistema.bancario.core.saldo.Conta;
import com.sistema.bancario.core.saldo.ContaRepository;
import com.sistema.bancario.core.ted.TedRepository;
import com.sistema.bancario.core.transferencia.TransferenciaService;

@SpringBootApplication
public class SistemaBancarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaBancarioApplication.class, args);
	}

	@Bean
	@Profile("!test")
	CommandLineRunner demonstracaoInicial(
			ContaRepository contaRepository,
			PixRepository pixRepository,
			BoletoRepository boletoRepository,
			TedRepository tedRepository,
			DocRepository docRepository,
			ItemLoteRepository itemLoteRepository,
			LoteService loteService,
			PagamentoService pagamentoService,
			TransferenciaService transferenciaService) {
		return args -> {
			System.out.println("\n==================================================");
			System.out.println("          SISTEMA BANCÁRIO - EXECUÇÃO             ");
			System.out.println("==================================================");

			if (contaRepository.count() == 0) {
				Conta origem = contaRepository.save(new Conta("0001-1", 2000.0, "joao@email.com"));
				Conta destino = contaRepository.save(new Conta("0002-2", 500.0, "12345678901"));
				System.out.println("[OK] Contas iniciais criadas (0001-1 e 0002-2).");

				pagamentoService.processarPagamento(origem.getId(), 150.0, "PIX", "loja@email.com");
				System.out.println("[OK] Pagamento PIX de R$ 150,00 concluído (loja@email.com).");

				pagamentoService.processarPagamento(origem.getId(), 100.0, "BOLETO", "12345678901234");
				System.out.println("[OK] Pagamento BOLETO de R$ 100,00 concluído (enfileirado no lote).");

				transferenciaService.processarTransferencia(origem.getId(), null, 200.0, "PIX", "12345678901");
				System.out.println("[OK] Transferência PIX de R$ 200,00 concluída (chave 12345678901).");

				transferenciaService.processarTransferencia(origem.getId(), destino.getId(), 300.0, "TED", null);
				System.out.println("[OK] Transferência TED de R$ 300,00 concluída (conta 0002-2).");

				transferenciaService.processarTransferencia(origem.getId(), destino.getId(), 250.0, "DOC", null);
				System.out.println("[OK] Transferência DOC de R$ 250,00 concluída (enfileirado no lote).");

				int processados = loteService.processarLotePendente();
				System.out.printf("[OK] Processamento do Lote concluído (%d itens processados).%n", processados);
			} else {
				System.out.println("[INFO] Banco já populado. Exibindo estado atual:");
			}

			System.out.println("\n--- Saldos Atuais ---");
			contaRepository.findAll().forEach(conta ->
					System.out.printf("Conta %-7s | Chave: %-15s | Saldo: R$ %.2f%n",
							conta.getNumeroConta(), conta.getChavePix(), conta.getSaldoAtual()));

			System.out.println("\n--- Registros Persistidos no SQLite ---");
			System.out.printf("Pix: %d | Boleto: %d | TED: %d | DOC: %d | Lotes: %d%n",
					pixRepository.count(),
					boletoRepository.count(),
					tedRepository.count(),
					docRepository.count(),
					itemLoteRepository.count());
			System.out.println("==================================================\n");
		};
	}

}
