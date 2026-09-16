import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CriarContrato from '../CriarContrato';

function renderPagina() {
  return render(
    <MemoryRouter>
      <CriarContrato />
    </MemoryRouter>
  );
}

describe('CriarContrato', () => {
  it('mostra a aba "Novo contrato" por padrão', () => {
    renderPagina();
    expect(screen.getByRole('heading', { name: 'Novo contrato' })).toBeInTheDocument();
    expect(screen.queryByText(/Converta um contrato entre PDF e Word/)).not.toBeInTheDocument();
  });

  it('troca para a aba "Converter" e volta pra "Novo contrato"', () => {
    renderPagina();

    fireEvent.click(screen.getByRole('button', { name: 'Converter' }));
    expect(screen.getByText(/Converta um contrato entre PDF e Word/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Novo contrato' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Novo contrato' }));
    expect(screen.getByRole('heading', { name: 'Novo contrato' })).toBeInTheDocument();
  });
});
