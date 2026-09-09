import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SignaturePad from '../SignaturePad';

// jsdom não implementa renderização 2D de canvas de verdade — mockamos o
// contexto com stubs, só pra exercitar a lógica de "desenhou algo ou não"
// do componente, não o desenho em si.
beforeEach(() => {
  HTMLCanvasElement.prototype.getContext = vi.fn(() => ({
    beginPath: vi.fn(),
    moveTo: vi.fn(),
    lineTo: vi.fn(),
    stroke: vi.fn(),
    clearRect: vi.fn(),
  }));
  HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/png;base64,ASSINATURA_FAKE');
});

describe('SignaturePad', () => {
  it('chama onChange com base64 depois de um traço real (mousedown+move+up)', () => {
    const onChange = vi.fn();
    render(<SignaturePad onChange={onChange} />);
    const canvas = document.querySelector('canvas');

    fireEvent.mouseDown(canvas, { clientX: 10, clientY: 10 });
    fireEvent.mouseMove(canvas, { clientX: 20, clientY: 20 });
    fireEvent.mouseUp(canvas);

    expect(onChange).toHaveBeenLastCalledWith('ASSINATURA_FAKE');
  });

  it('chama onChange com null quando não houve nenhum movimento (clique sem arrastar)', () => {
    const onChange = vi.fn();
    render(<SignaturePad onChange={onChange} />);
    const canvas = document.querySelector('canvas');

    fireEvent.mouseDown(canvas, { clientX: 10, clientY: 10 });
    fireEvent.mouseUp(canvas);

    expect(onChange).toHaveBeenLastCalledWith(null);
  });

  it('"Limpar assinatura" volta a chamar onChange com null', () => {
    const onChange = vi.fn();
    render(<SignaturePad onChange={onChange} />);
    const canvas = document.querySelector('canvas');

    fireEvent.mouseDown(canvas, { clientX: 10, clientY: 10 });
    fireEvent.mouseMove(canvas, { clientX: 20, clientY: 20 });
    fireEvent.mouseUp(canvas);
    expect(onChange).toHaveBeenLastCalledWith('ASSINATURA_FAKE');

    fireEvent.click(screen.getByText('Limpar assinatura'));
    expect(onChange).toHaveBeenLastCalledWith(null);
  });
});
