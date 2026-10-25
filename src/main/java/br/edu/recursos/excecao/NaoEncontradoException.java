package br.edu.recursos.excecao;

/** Recurso ou reserva inexistente (HTTP 404). */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
