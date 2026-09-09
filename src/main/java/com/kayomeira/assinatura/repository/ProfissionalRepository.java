package com.kayomeira.assinatura.repository;

import com.kayomeira.assinatura.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
    Optional<Profissional> findByEmail(String email);
    Optional<Profissional> findByGoogleId(String googleId);
    Optional<Profissional> findByTokenVerificacaoEmail(String token);
    Optional<Profissional> findByTokenResetSenha(String token);
}
