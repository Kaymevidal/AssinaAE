import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../../context/AuthContext';
import PainelProfissional from '../PainelProfissional';
import * as api from '../../services/api';

vi.spyOn(api, 'meusContratos').mockResolvedValue({
  content: [],
  number: 0,
  totalPages: 0,
  first: true,
  last: true,
});

function renderPainel(abaInicial) {
  localStorage.setItem('assinatura_digital_token', 'token-fake');
  localStorage.setItem(
    'assinatura_digital_profissional',
    JSON.stringify({ id: 1, nome: 'Ana', email: 'ana@teste.com', emailVerificado: true })
  );

  return render(
    <MemoryRouter>
      <AuthProvider>
        <PainelProfissional abaInicial={abaInicial} />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('PainelProfissional', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('mostra a aba "Meus contratos" por padrão', () => {
    renderPainel();
    expect(screen.getByRole('button', { name: 'Meus contratos' })).toHaveClass('abas-documento__item--ativa');
    expect(screen.queryByText(/Converta um contrato do Word/)).not.toBeInTheDocument();
  });

  it('aceita abrir direto na aba "Novo contrato"', () => {
    renderPainel('novo-contrato');
    expect(screen.getByRole('heading', { name: 'Novo contrato' })).toBeInTheDocument();
  });

  it('troca para a aba "Converter" e volta pra "Novo contrato"', () => {
    renderPainel();

    fireEvent.click(screen.getByRole('button', { name: 'Converter' }));
    expect(screen.getByText(/Converta um contrato do Word para PDF/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Novo contrato' }));
    expect(screen.getByRole('heading', { name: 'Novo contrato' })).toBeInTheDocument();
  });
});
