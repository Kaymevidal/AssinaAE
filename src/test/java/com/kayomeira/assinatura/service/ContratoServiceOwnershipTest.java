package com.kayomeira.assinatura.service;

import com.kayomeira.assinatura.exception.ContratoNotFoundException;
import com.kayomeira.assinatura.model.Contrato;
import com.kayomeira.assinatura.model.Profissional;
import com.kayomeira.assinatura.repository.ContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Um profissional não pode enxergar nem baixar o contrato de outro só por
 * adivinhar o id — {@link ContratoService#buscarPorIdDoProfissional} deve
 * tratar "existe mas não é seu" exatamente como "não existe".
 */
class ContratoServiceOwnershipTest {

    @Mock
    private ContratoRepository contratoRepository;

    private ContratoService contratoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        contratoService = new ContratoService(contratoRepository, new AssinaturaPDFService(), new EmailService());
    }

    @Test
    void profissionalDonoConsegueAcessarOProprioContrato() {
        Profissional dono = Profissional.builder().id(1L).nome("Ana").email("ana@teste.com").build();
        Contrato contrato = Contrato.builder().id(10L).profissional(dono).build();
        when(contratoRepository.findById(10L)).thenReturn(Optional.of(contrato));

        assertThat(contratoService.buscarPorIdDoProfissional(10L, 1L)).isEqualTo(contrato);
    }

    @Test
    void profissionalDiferenteNaoConsegueAcessarContratoAlheio() {
        Profissional dono = Profissional.builder().id(1L).nome("Ana").email("ana@teste.com").build();
        Contrato contrato = Contrato.builder().id(10L).profissional(dono).build();
        when(contratoRepository.findById(10L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.buscarPorIdDoProfissional(10L, 2L))
                .isInstanceOf(ContratoNotFoundException.class);
    }

    @Test
    void profissionalDonoConsegueExcluirOProprioContrato() {
        Profissional dono = Profissional.builder().id(1L).nome("Ana").email("ana@teste.com").build();
        Contrato contrato = Contrato.builder().id(10L).profissional(dono).build();
        when(contratoRepository.findById(10L)).thenReturn(Optional.of(contrato));

        contratoService.excluirContrato(10L, 1L);

        verify(contratoRepository).delete(contrato);
    }

    @Test
    void profissionalDiferenteNaoConsegueExcluirContratoAlheio() {
        Profissional dono = Profissional.builder().id(1L).nome("Ana").email("ana@teste.com").build();
        Contrato contrato = Contrato.builder().id(10L).profissional(dono).build();
        when(contratoRepository.findById(10L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.excluirContrato(10L, 2L))
                .isInstanceOf(ContratoNotFoundException.class);

        verify(contratoRepository, never()).delete(contrato);
    }
}
