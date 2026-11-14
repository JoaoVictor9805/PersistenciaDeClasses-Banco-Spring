package com.sistema.bancario.core.lotes;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ItemLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoModalidade;

    private Long idContaOrigem;

    private String destino;

    private Double valor;

    private String status;

    private LocalDateTime dataCriacao;

    public ItemLote() {
    }

    public ItemLote(String tipoModalidade, Long idContaOrigem, String destino, Double valor) {
        this.tipoModalidade = tipoModalidade;
        this.idContaOrigem = idContaOrigem;
        this.destino = destino;
        this.valor = valor;
        this.status = "PENDENTE";
        this.dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipoModalidade() {
        return tipoModalidade;
    }

    public void setTipoModalidade(String tipoModalidade) {
        this.tipoModalidade = tipoModalidade;
    }

    public Long getIdContaOrigem() {
        return idContaOrigem;
    }

    public void setIdContaOrigem(Long idContaOrigem) {
        this.idContaOrigem = idContaOrigem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
