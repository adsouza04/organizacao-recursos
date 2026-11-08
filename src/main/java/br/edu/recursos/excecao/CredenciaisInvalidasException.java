package br.edu.recursos.excecao;

/** Usuário inexistente, inativo ou senha incorreta (HTTP 401). Mensagem única para não revelar qual falhou. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Usuário ou senha inválidos");
    }
}