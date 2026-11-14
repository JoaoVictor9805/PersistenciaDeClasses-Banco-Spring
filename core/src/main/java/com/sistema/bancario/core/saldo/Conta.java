package com.sistema.bancario.core.saldo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroConta;

    private Double saldoAtual;

    @Column(unique = true)
    private String chavePix;

    // Construtor vazio obrigatório para o Spring (JPA) funcionar
    public Conta() {
    }

    // Construtor com parâmetros para facilitar testes rápidos depois
    public Conta(String numeroConta, Double saldoAtual) {
        this.numeroConta = numeroConta;
        this.saldoAtual = saldoAtual;
    }

    public Conta(String numeroConta, Double saldoAtual, String chavePix) {
        this.numeroConta = numeroConta;
        this.saldoAtual = saldoAtual;
        this.chavePix = chavePix;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public void setNumeroConta(String numeroConta) {
        this.numeroConta = numeroConta;
    }

    public Double getSaldoAtual() {
        return saldoAtual;
    }

    public void setSaldoAtual(Double saldoAtual) {
        this.saldoAtual = saldoAtual;
    }

    public String getChavePix() {
        return chavePix;
    }

    public void setChavePix(String chavePix) {
        this.chavePix = chavePix;
    }
}