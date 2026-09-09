import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from '../AuthContext';
import * as api from '../../services/api';

vi.mock('../../services/api', async () => {
  const real = await vi.importActual('../../services/api');
  return {
    ...real,
    login: vi.fn(),
    registrar: vi.fn(),
    loginGoogle: vi.fn(),
  };
});

function TelaDeTeste() {
  const { profissional, autenticado, entrar, sair } = useAuth();
  return (
    <div>
      <span data-testid="autenticado">{String(autenticado)}</span>
      <span data-testid="nome">{profissional?.nome ?? ''}</span>
      <button onClick={() => entrar({ email: 'ana@teste.com', senha: 'x' })}>Entrar</button>
      <button onClick={sair}>Sair</button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('entrar() guarda o token e os dados do profissional, mas nunca a senha', async () => {
    api.login.mockResolvedValue({
      token: 'token-fake-123',
      id: 1,
      nome: 'Ana',
      email: 'ana@teste.com',
      emailVerificado: true,
    });

    render(<AuthProvider><TelaDeTeste /></AuthProvider>);
    await userEvent.click(screen.getByText('Entrar'));

    await waitFor(() => expect(screen.getByTestId('autenticado').textContent).toBe('true'));
    expect(screen.getByTestId('nome').textContent).toBe('Ana');

    expect(localStorage.getItem(api.TOKEN_KEY)).toBe('token-fake-123');
    const salvo = JSON.parse(localStorage.getItem('assinatura_digital_profissional'));
    expect(salvo).toEqual({ id: 1, nome: 'Ana', email: 'ana@teste.com', emailVerificado: true });
    expect(JSON.stringify(salvo)).not.toContain('senha');
  });

  it('sair() limpa o localStorage e o estado', async () => {
    api.login.mockResolvedValue({ token: 't', id: 1, nome: 'Ana', email: 'ana@teste.com', emailVerificado: true });

    render(<AuthProvider><TelaDeTeste /></AuthProvider>);
    await userEvent.click(screen.getByText('Entrar'));
    await waitFor(() => expect(screen.getByTestId('autenticado').textContent).toBe('true'));

    await userEvent.click(screen.getByText('Sair'));

    expect(screen.getByTestId('autenticado').textContent).toBe('false');
    expect(localStorage.getItem(api.TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem('assinatura_digital_profissional')).toBeNull();
  });
});
