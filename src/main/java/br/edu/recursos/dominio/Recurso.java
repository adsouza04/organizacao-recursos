package br.edu.recursos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "recurso")
public class Recurso {

    @Id
    private UUID id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoRecurso tipo;

    private boolean restrito;

    @Enumerated(EnumType.STRING)
    private StatusRecurso status;

    @Version
    @Column(name = "versao")
    private long versao;

    protected Recurso() {
    }

    public Recurso(String nome, TipoRecurso tipo, boolean restrito) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.tipo = tipo;
        this.restrito = restrito;
        this.status = StatusRecurso.ATIVO;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public boolean isRestrito() {
        return restrito;
    }

    public StatusRecurso getStatus() {
        return status;
    }

    public boolean emManutencao() {
        return status == StatusRecurso.MANUTENCAO;
    }

    public void entrarEmManutencao() {
        this.status = StatusRecurso.MANUTENCAO;
    }

    public void sairDaManutencao() {
        this.status = StatusRecurso.ATIVO;
    }
}
