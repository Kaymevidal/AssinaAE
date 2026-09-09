import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RotaProtegida from '../RotaProtegida';
import * as AuthContext from '../../context/AuthContext';

function renderComAutenticado(autenticado) {
  vi.spyOn(AuthContext, 'useAuth').mockReturnValue({ autenticado });

  return render(
    <MemoryRouter initialEntries={['/protegida']}>
      <Routes>
        <Route path="/login" element={<div>Página de login</div>} />
        <Route
          path="/protegida"
          element={
            <RotaProtegida>
              <div>Conteúdo protegido</div>
            </RotaProtegida>
          }
        />
      </Routes>
    </MemoryRouter>
  );
}

describe('RotaProtegida', () => {
  it('redireciona para /login quando não autenticado', () => {
    renderComAutenticado(false);
    expect(screen.getByText('Página de login')).toBeInTheDocument();
    expect(screen.queryByText('Conteúdo protegido')).not.toBeInTheDocument();
  });

  it('renderiza os filhos quando autenticado', () => {
    renderComAutenticado(true);
    expect(screen.getByText('Conteúdo protegido')).toBeInTheDocument();
    expect(screen.queryByText('Página de login')).not.toBeInTheDocument();
  });
});
