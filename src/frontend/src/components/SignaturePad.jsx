import { useRef, useEffect } from 'react';

export default function SignaturePad({ onChange }) {
  const canvasRef = useRef(null);
  const isDrawing = useRef(false);
  const temDesenho = useRef(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#1a1a1a';
  }, []);

  function posicaoDoEvento(evento) {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const ponto = evento.touches ? evento.touches[0] : evento;
    return {
      x: ponto.clientX - rect.left,
      y: ponto.clientY - rect.top,
    };
  }

  function iniciarTraco(evento) {
    evento.preventDefault();
    isDrawing.current = true;
    const { x, y } = posicaoDoEvento(evento);
    const ctx = canvasRef.current.getContext('2d');
    ctx.beginPath();
    ctx.moveTo(x, y);
  }

  function desenhar(evento) {
    if (!isDrawing.current) return;
    evento.preventDefault();
    const { x, y } = posicaoDoEvento(evento);
    const ctx = canvasRef.current.getContext('2d');
    ctx.lineTo(x, y);
    ctx.stroke();
    temDesenho.current = true;
  }

  function finalizarTraco() {
    if (!isDrawing.current) return;
    isDrawing.current = false;
    exportar();
  }

  function exportar() {
    if (!temDesenho.current) {
      onChange(null);
      return;
    }
    const canvas = canvasRef.current;
    const base64 = canvas.toDataURL('image/png').split(',')[1];
    onChange(base64);
  }

  function limpar() {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    temDesenho.current = false;
    onChange(null);
  }

  return (
    <div className="signature-pad">
      <canvas
        ref={canvasRef}
        width={400}
        height={180}
        className="signature-pad__canvas"
        onMouseDown={iniciarTraco}
        onMouseMove={desenhar}
        onMouseUp={finalizarTraco}
        onMouseLeave={finalizarTraco}
        onTouchStart={iniciarTraco}
        onTouchMove={desenhar}
        onTouchEnd={finalizarTraco}
      />
      <button type="button" className="signature-pad__limpar" onClick={limpar}>
        Limpar assinatura
      </button>
    </div>
  );
}
