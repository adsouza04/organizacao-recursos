package br.edu.recursos.excecao;

/** Conflito de horário / dupla reserva (HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
