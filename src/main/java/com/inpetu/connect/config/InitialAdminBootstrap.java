package com.inpetu.connect.config;

import com.inpetu.connect.model.Organizacao;
import com.inpetu.connect.model.Usuario;
import com.inpetu.connect.repository.OrganizacaoRepository;
import com.inpetu.connect.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@Profile("!demo")
public class InitialAdminBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    @Bean
    ApplicationRunner bootstrapAdmin(
        UsuarioRepository usuarios,
        OrganizacaoRepository organizacoes,
        PasswordEncoder passwordEncoder,
        @Value("${app.bootstrap.admin.email:}") String email,
        @Value("${app.bootstrap.admin.password:}") String password
    ) {
        return (ApplicationArguments args) -> {
            boolean developerExistente = usuarios.existsByIdPerfil(5);
            boolean contaExistente = false;
            if (!developerExistente && email != null && !email.isBlank()
                    && password != null && !password.isBlank()) {
                String developerEmail = email.trim();
                if (usuarios.findByEmailIgnoreCase(developerEmail).isEmpty()) {
                    if (password.length() < 12 || password.length() > 72) {
                        throw new IllegalStateException("APP_ADMIN_PASSWORD deve conter entre 12 e 72 caracteres.");
                    }
                    Organizacao organizacao = organizacoes.findByNomeOrganizacaoIgnoreCase("UFSC / InPETU Hub")
                            .or(() -> organizacoes.findByAtivoTrueOrderByNomeOrganizacao().stream().findFirst())
                            .orElse(null);
                    Usuario developer = new Usuario();
                    developer.setNomeCompleto("Desenvolvedor / Admin");
                    developer.setEmail(developerEmail);
                    developer.setCargoVinculo("Desenvolvimento e administração");
                    developer.setSenhaHash(passwordEncoder.encode(password));
                    developer.setIdPerfil(5);
                    developer.setIdPersona(1);
                    if (organizacao != null) {
                        developer.setIdOrganizacao(organizacao.getIdOrganizacao());
                    }
                    developer.setAtivo(true);
                    usuarios.save(developer);
                    developerExistente = true;
                } else {
                    contaExistente = true;
                }
            }
            if (developerExistente || contaExistente || usuarios.existsByIdPerfil(1)) return;
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                logger.warn("Nenhum coordenador foi criado: configure APP_ADMIN_EMAIL e APP_ADMIN_PASSWORD para inicializar o primeiro coordenador.");
                return;
            }
            if (password.length() < 12 || password.length() > 72) {
                throw new IllegalStateException("APP_ADMIN_PASSWORD deve conter entre 12 e 72 caracteres para criar o primeiro coordenador.");
            }
            String adminEmail = email.trim();

            Organizacao organizacao = organizacoes.findByNomeOrganizacaoIgnoreCase("UFSC / InPETU Hub")
                    .or(() -> organizacoes.findByAtivoTrueOrderByNomeOrganizacao().stream().findFirst())
                    .orElse(null);
            Usuario admin = new Usuario();
            admin.setNomeCompleto("Coordenador InPETU");
            admin.setEmail(adminEmail);
            admin.setCargoVinculo("Coordenação");
            admin.setSenhaHash(passwordEncoder.encode(password));
            admin.setIdPerfil(1);
            admin.setIdPersona(1);
            if (organizacao != null) {
                admin.setIdOrganizacao(organizacao.getIdOrganizacao());
            }
            admin.setAtivo(true);
            usuarios.save(admin);
        };
    }
}
