package com.kayomeira.assinatura.repository;

import com.kayomeira.assinatura.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    Optional<Contrato> findByTokenProfissional(String token);
    Optional<Contrato> findByTokenCliente(String token);
    Optional<Contrato> findByEmailProfissionalAndEmailCliente(String emailProfissional, String emailCliente);
}
