import { useEffect, useRef, useState } from 'react';
import { carregarPdfjs } from '../lib/pdfjs';

// A assinatura sempre vem do SignaturePad, cujo canvas é fixo em 400x180 —
// manter essa proporção no redimensionamento evita distorcer o traço.
const PROPORCAO_ASSINATURA = 400 / 180;
const LARGURA_MINIMA_FRACAO = 0.08;
const MARGEM_PADRAO_FRACAO = 0.06;
const LARGURA_PADRAO_FRACAO = 0.3;
// Índice de página fora do range real (qualquer PDF) — o backend cai pra
// última página automaticamente. Serve de valor inicial e de fallback caso
// o pdf.js não consiga carregar a pré-visualização: a assinatura ainda
// pode ser confirmada, só sem o posicionamento visual.
export const ULTIMA_PAGINA_SENTINELA = 999999;

function clamp(valor, min, max) {
  return Math.max(min, Math.min(max, valor));
}

export function caixaPadrao(ladoDireito) {
  const largura = LARGURA_PADRAO_FRACAO;
  const altura = largura / PROPORCAO_ASSINATURA;
  const x = ladoDireito ? 1 - largura - MARGEM_PADRAO_FRACAO : MARGEM_PADRAO_FRACAO;
  const y = 1 - altura - MARGEM_PADRAO_FRACAO;
  return { x, y, largura, altura };
}

/**
 * Mostra o PDF do contrato e deixa quem está assinando arrastar/redimensionar
 * a própria assinatura por cima, em qualquer página. `onChange` recebe
 * `{ pagina, x, y, largura, altura }` sempre em fração da página (0 a 1),
 * o que a torna independente do zoom/tamanho de tela usado aqui.
 */
export default function PosicionadorAssinatura({ pdfBytes, assinaturaBase64, ladoInicialDireito, onChange }) {
  const [pdfDoc, setPdfDoc] = useState(null);
  const [numPaginas, setNumPaginas] = useState(0);
  const [paginaAtual, setPaginaAtual] = useState(ULTIMA_PAGINA_SENTINELA);
  const [caixa, setCaixa] = useState(() => caixaPadrao(ladoInicialDireito));
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  const wrapperRef = useRef(null);
  const canvasRef = useRef(null);

  useEffect(() => {
    let cancelado = false;

    async function carregar() {
      setCarregando(true);
      setErro('');
      try {
        const pdfjsLib = await carregarPdfjs();
        // slice() garante um buffer independente: pdf.js pode "transferir" o
        // ArrayBuffer original, o que o deixaria vazio numa segunda tentativa.
        const doc = await pdfjsLib.getDocument({ data: pdfBytes.slice(0) }).promise;
        if (cancelado) return;
        setPdfDoc(doc);
        setNumPaginas(doc.numPages);
        setPaginaAtual(doc.numPages - 1);
      } catch {
        if (!cancelado) setErro('Não foi possível carregar a pré-visualização do PDF. Você ainda pode assinar, mas não vai dar pra posicionar visualmente.');
      } finally {
        if (!cancelado) setCarregando(false);
      }
    }

    carregar();
    return () => {
      cancelado = true;
    };
  }, [pdfBytes]);

  useEffect(() => {
    if (!pdfDoc) return;
    let cancelado = false;

    async function renderizar() {
      const page = await pdfDoc.getPage(paginaAtual + 1);
      if (cancelado) return;

      const larguraAlvo = Math.min(wrapperRef.current?.clientWidth || 480, 480);
      const dpr = window.devicePixelRatio || 1;
      const escalaBase = larguraAlvo / page.getViewport({ scale: 1 }).width;
      const viewport = page.getViewport({ scale: escalaBase * dpr });

      const canvas = canvasRef.current;
      const contexto = canvas.getContext('2d');
      canvas.width = viewport.width;
      canvas.height = viewport.height;
      canvas.style.width = `${larguraAlvo}px`;
      canvas.style.height = `${viewport.height / dpr}px`;

      await page.render({ canvasContext: contexto, viewport }).promise;
    }

    renderizar();
    return () => {
      cancelado = true;
    };
  }, [pdfDoc, paginaAtual]);

  useEffect(() => {
    onChange({ pagina: paginaAtual, ...caixa });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paginaAtual, caixa]);

  function atualizarCaixa(novaCaixa) {
    setCaixa({
      x: clamp(novaCaixa.x, 0, 1 - novaCaixa.largura),
      y: clamp(novaCaixa.y, 0, 1 - novaCaixa.altura),
      largura: novaCaixa.largura,
      altura: novaCaixa.altura,
    });
  }

  function iniciarArraste(evento) {
    evento.preventDefault();
    const rect = wrapperRef.current.getBoundingClientRect();
    const ponto = evento.touches ? evento.touches[0] : evento;
    const inicioX = ponto.clientX;
    const inicioY = ponto.clientY;
    const caixaInicial = caixa;

    function mover(e) {
      e.preventDefault();
      const p = e.touches ? e.touches[0] : e;
      const deltaX = (p.clientX - inicioX) / rect.width;
      const deltaY = (p.clientY - inicioY) / rect.height;
      atualizarCaixa({
        ...caixaInicial,
        x: caixaInicial.x + deltaX,
        y: caixaInicial.y + deltaY,
      });
    }

    function soltar() {
      window.removeEventListener('mousemove', mover);
      window.removeEventListener('mouseup', soltar);
      window.removeEventListener('touchmove', mover);
      window.removeEventListener('touchend', soltar);
    }

    window.addEventListener('mousemove', mover);
    window.addEventListener('mouseup', soltar);
    window.addEventListener('touchmove', mover, { passive: false });
    window.addEventListener('touchend', soltar);
  }

  function iniciarRedimensionamento(evento) {
    evento.preventDefault();
    evento.stopPropagation();
    const rect = wrapperRef.current.getBoundingClientRect();
    const ponto = evento.touches ? evento.touches[0] : evento;
    const inicioX = ponto.clientX;
    const caixaInicial = caixa;
    const alturaMaxima = 1 - caixaInicial.y;
    const larguraMaxima = Math.min(1 - caixaInicial.x, alturaMaxima * PROPORCAO_ASSINATURA * (rect.height / rect.width));

    function mover(e) {
      e.preventDefault();
      const p = e.touches ? e.touches[0] : e;
      const deltaX = (p.clientX - inicioX) / rect.width;
      const novaLargura = clamp(caixaInicial.largura + deltaX, LARGURA_MINIMA_FRACAO, larguraMaxima);
      const novaAltura = (novaLargura * rect.width) / PROPORCAO_ASSINATURA / rect.height;
      setCaixa({ ...caixaInicial, largura: novaLargura, altura: novaAltura });
    }

    function soltar() {
      window.removeEventListener('mousemove', mover);
      window.removeEventListener('mouseup', soltar);
      window.removeEventListener('touchmove', mover);
      window.removeEventListener('touchend', soltar);
    }

    window.addEventListener('mousemove', mover);
    window.addEventListener('mouseup', soltar);
    window.addEventListener('touchmove', mover, { passive: false });
    window.addEventListener('touchend', soltar);
  }

  return (
    <div className="posicionador">
      {numPaginas > 1 && (
        <div className="posicionador__paginacao">
          <button type="button" onClick={() => setPaginaAtual((p) => p - 1)} disabled={paginaAtual === 0}>
            ‹ Página anterior
          </button>
          <span>Página {paginaAtual + 1} de {numPaginas}</span>
          <button type="button" onClick={() => setPaginaAtual((p) => p + 1)} disabled={paginaAtual === numPaginas - 1}>
            Próxima página ›
          </button>
        </div>
      )}

      {erro && <p className="erro">{erro}</p>}
      {carregando && <p className="posicionador__carregando">Carregando pré-visualização do PDF...</p>}

      {pdfDoc && (
        <>
          <div className="posicionador__pagina" ref={wrapperRef}>
            <canvas ref={canvasRef} className="posicionador__canvas" />
            <div
              className="posicionador__assinatura"
              style={{
                left: `${caixa.x * 100}%`,
                top: `${caixa.y * 100}%`,
                width: `${caixa.largura * 100}%`,
                height: `${caixa.altura * 100}%`,
              }}
              onMouseDown={iniciarArraste}
              onTouchStart={iniciarArraste}
            >
              <img src={`data:image/png;base64,${assinaturaBase64}`} alt="Sua assinatura" draggable={false} />
              <div
                className="posicionador__redimensionar"
                onMouseDown={iniciarRedimensionamento}
                onTouchStart={iniciarRedimensionamento}
              />
            </div>
          </div>
          <p className="posicionador__dica">Arraste a assinatura para posicioná-la e use o canto para redimensionar.</p>
        </>
      )}
    </div>
  );
}
