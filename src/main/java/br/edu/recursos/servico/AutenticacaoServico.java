package br.edu.recursos.servico;

import br.edu.recursos.dominio.Usuario;
import br.edu.recursos.excecao.CredenciaisInvalidasException;
import br.edu.recursos.repositorio.UsuarioRepositorio;
import br.edu.recursos.seguranca.EmissorToken;
import br.edu.recursos.seguranca.TokenEmitido;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoServico {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificadorSenha;
    private final EmissorToken emissorToken;

    public AutenticacaoServico(UsuarioRepositorio usuarios, PasswordEncoder codificadorSenha,
                               EmissorToken emissorToken) {
        this.usuarios = usuarios;
        this.codificadorSenha = codificadorSenha;
        this.emissorToken = emissorToken;
    }

    @Transactional(readOnly = true)
    public TokenEmitido autenticar(String login, String senha) {
        Usuario usuario = usuarios.findByLoginAndAtivoTrue(login)
                .filter(u -> codificadorSenha.matches(senha, u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return emissorToken.emitir(usuario.getLogin(), usuario.getPerfil());
    }
}