import { useEffect, useRef, useState } from 'react';
import { carregarPdfjs } from '../lib/pdfjs';

/**
 * Visualizador somente leitura: renderiza todas as páginas do PDF em coluna
 * rolável (diferente do `PosicionadorAssinatura`, que mostra uma página por
 * vez). Chama `onLeituraCompleta` quando a última página entra em vista —
 * usado pra liberar a etapa de assinatura só depois que o documento inteiro
 * foi visto.
 */
export default function VisualizadorPdf({ pdfBytes, onLeituraCompleta }) {
  const [pdfDoc, setPdfDoc] = useState(null);
  const [numPaginas, setNumPaginas] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  const canvasRefs = useRef([]);
  const containerRef = useRef(null);
  const ultimaPaginaRef = useRef(null);
  const jaNotificouRef = useRef(false);

  useEffect(() => {
    let cancelado = false;

    async function carregar() {
      setCarregando(true);
      setErro('');
      try {
        const pdfjsLib = await carregarPdfjs();
        const doc = await pdfjsLib.getDocument({ data: pdfBytes.slice(0) }).promise;
        if (cancelado) return;
        setPdfDoc(doc);
        setNumPaginas(doc.numPages);
      } catch {
        if (!cancelado) setErro('Não foi possível carregar o documento para leitura.');
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

    async function renderizarPaginas() {
      for (let i = 1; i <= pdfDoc.numPages; i++) {
        if (cancelado) return;
        const canvas = canvasRefs.current[i - 1];
        if (!canvas) continue;

        const page = await pdfDoc.getPage(i);
        const larguraAlvo = Math.min(canvas.parentElement?.clientWidth || 480, 640);
        const dpr = window.devicePixelRatio || 1;
        const escalaBase = larguraAlvo / page.getViewport({ scale: 1 }).width;
        const viewport = page.getViewport({ scale: escalaBase * dpr });

        canvas.width = viewport.width;
        canvas.height = viewport.height;
        canvas.style.width = `${larguraAlvo}px`;
        canvas.style.height = `${viewport.height / dpr}px`;

        await page.render({ canvasContext: canvas.getContext('2d'), viewport }).promise;
      }
    }

    renderizarPaginas();
    return () => {
      cancelado = true;
    };
  }, [pdfDoc]);

  useEffect(() => {
    if (!pdfDoc || !ultimaPaginaRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting) && !jaNotificouRef.current) {
          jaNotificouRef.current = true;
          onLeituraCompleta?.();
        }
      },
      // `root` explícito no container rolável: sem isso o IntersectionObserver
      // considera a viewport inteira, e a última página pode "intersectar" mesmo
      // sem o usuário ter rolado o suficiente dentro da caixa de pré-visualização.
      { root: containerRef.current, threshold: 0.6 },
    );

    observer.observe(ultimaPaginaRef.current);
    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pdfDoc, numPaginas]);

  return (
    <div className="visualizador">
      {erro && <p className="erro">{erro}</p>}
      {carregando && <p className="visualizador__carregando">Carregando documento...</p>}

      {pdfDoc && (
        <div className="visualizador__paginas" ref={containerRef}>
          {Array.from({ length: numPaginas }, (_, indice) => (
            <canvas
              key={indice}
              ref={(elemento) => {
                canvasRefs.current[indice] = elemento;
                if (indice === numPaginas - 1) ultimaPaginaRef.current = elemento;
              }}
              className="visualizador__canvas"
            />
          ))}
        </div>
      )}
    </div>
  );
}
