import { useEffect, useRef, useState } from 'react';
import { carregarPdfjs } from '../lib/pdfjs';
import { editarPdf } from '../services/api';

/**
 * Editor de PDF: clica em cima de um texto do documento e edita ali mesmo.
 * Não é reflow de texto de verdade — cobre a posição original e escreve o
 * texto novo por cima (EdicaoPdfService, no backend), a mesma técnica que
 * editores de PDF usam na prática. Cada item vem do textContent do pdf.js,
 * então a granularidade do clique é a mesma dos "runs" de texto do PDF
 * (geralmente palavras/trechos, não necessariamente a linha inteira).
 */
export default function EditorPdf({ pdfBytes, onSalvar, onCancelar }) {
  const [pdfDoc, setPdfDoc] = useState(null);
  const [numPaginas, setNumPaginas] = useState(0);
  const [paginas, setPaginas] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [edicoes, setEdicoes] = useState({});
  const [itemEmEdicao, setItemEmEdicao] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [erroSalvar, setErroSalvar] = useState('');

  const canvasRefs = useRef([]);
  const pdfjsLibRef = useRef(null);

  useEffect(() => {
    let cancelado = false;

    async function carregar() {
      setCarregando(true);
      setErro('');
      try {
        const pdfjsLib = await carregarPdfjs();
        pdfjsLibRef.current = pdfjsLib;
        const doc = await pdfjsLib.getDocument({ data: pdfBytes.slice(0) }).promise;
        if (cancelado) return;
        setPdfDoc(doc);
        setNumPaginas(doc.numPages);
      } catch {
        if (!cancelado) setErro('Não foi possível carregar o PDF para edição.');
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

    async function renderizarEExtrairTexto() {
      const pdfjsLib = pdfjsLibRef.current;
      const novasPaginas = [];

      for (let i = 1; i <= pdfDoc.numPages; i++) {
        if (cancelado) return;
        const canvas = canvasRefs.current[i - 1];
        if (!canvas) continue;

        // cada página é isolada: se uma falhar ao renderizar/extrair texto, as outras continuam editáveis
        try {
          const page = await pdfDoc.getPage(i);
          const larguraAlvo = Math.min(canvas.parentElement?.clientWidth || 480, 640);
          const escala = larguraAlvo / page.getViewport({ scale: 1 }).width;
          const viewport = page.getViewport({ scale: escala });

          canvas.width = viewport.width;
          canvas.height = viewport.height;
          canvas.style.width = `${larguraAlvo}px`;
          canvas.style.height = `${viewport.height}px`;

          await page.render({ canvasContext: canvas.getContext('2d'), viewport }).promise;

          const textContent = await page.getTextContent();
          const itens = textContent.items
            .filter((item) => item.str && item.str.trim().length > 0)
            .map((item) => {
              // mesma técnica que o textLayerBuilder do próprio pdf.js usa pra posicionar a camada de texto
              const tx = pdfjsLib.Util.transform(viewport.transform, item.transform);
              const alturaPx = Math.hypot(tx[2], tx[3]);
              const larguraPx = item.width * escala;
              const topoPx = tx[5] - alturaPx;

              return {
                texto: item.str,
                x: tx[4] / viewport.width,
                y: topoPx / viewport.height,
                largura: larguraPx / viewport.width,
                altura: alturaPx / viewport.height,
              };
            })
            .filter(
              (item) =>
                Number.isFinite(item.x) && Number.isFinite(item.y) &&
                Number.isFinite(item.largura) && Number.isFinite(item.altura) &&
                item.largura > 0 && item.altura > 0
            );

          novasPaginas.push({ itens });
        } catch {
          novasPaginas.push({ itens: [] });
        }
      }

      if (!cancelado) setPaginas(novasPaginas);
    }

    renderizarEExtrairTexto();
    return () => {
      cancelado = true;
    };
  }, [pdfDoc]);

  function textoAtual(chave, textoOriginal) {
    return chave in edicoes ? edicoes[chave] : textoOriginal;
  }

  function confirmarEdicao(chave, textoOriginal, novoTexto) {
    setItemEmEdicao(null);
    if (novoTexto === textoOriginal) return;
    setEdicoes((atual) => ({ ...atual, [chave]: novoTexto }));
  }

  async function salvar() {
    const listaEdicoes = [];
    paginas.forEach((paginaInfo, pagina) => {
      paginaInfo.itens.forEach((item, indice) => {
        const chave = `${pagina}-${indice}`;
        if (chave in edicoes && edicoes[chave] !== item.texto) {
          listaEdicoes.push({
            pagina,
            x: item.x,
            y: item.y,
            largura: item.largura,
            altura: item.altura,
            texto: edicoes[chave],
          });
        }
      });
    });

    if (listaEdicoes.length === 0) {
      onSalvar(null);
      return;
    }

    setSalvando(true);
    setErroSalvar('');
    try {
      const pdfOriginal = new File([pdfBytes], 'original.pdf', { type: 'application/pdf' });
      const blob = await editarPdf(pdfOriginal, listaEdicoes);
      onSalvar(blob);
    } catch (e) {
      setErroSalvar(e.response?.data?.erro || 'Não foi possível salvar as edições');
    } finally {
      setSalvando(false);
    }
  }

  const totalEdicoes = Object.keys(edicoes).length;

  return (
    <div className="editor-pdf">
      <p>Clique em cima de qualquer texto do documento pra editar.</p>

      {erro && <p className="erro">{erro}</p>}
      {carregando && <p className="visualizador__carregando">Carregando documento...</p>}

      {pdfDoc && (
        <div className="visualizador__paginas">
          {Array.from({ length: numPaginas }, (_, indicePagina) => (
            <div key={indicePagina} className="editor-pdf__pagina">
              <canvas
                ref={(elemento) => {
                  canvasRefs.current[indicePagina] = elemento;
                }}
                className="visualizador__canvas"
              />
              {paginas[indicePagina] && (
                <div className="editor-pdf__overlay">
                  {paginas[indicePagina].itens.map((item, indice) => {
                    const chave = `${indicePagina}-${indice}`;
                    const texto = textoAtual(chave, item.texto);
                    const estilo = {
                      left: `${item.x * 100}%`,
                      top: `${item.y * 100}%`,
                      width: `${item.largura * 100}%`,
                      height: `${item.altura * 100}%`,
                    };

                    if (itemEmEdicao === chave) {
                      return (
                        <input
                          key={chave}
                          className="editor-pdf__input"
                          style={estilo}
                          autoFocus
                          defaultValue={texto}
                          onBlur={(e) => confirmarEdicao(chave, item.texto, e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') e.target.blur();
                            if (e.key === 'Escape') setItemEmEdicao(null);
                          }}
                        />
                      );
                    }

                    return (
                      <button
                        key={chave}
                        type="button"
                        className={`editor-pdf__item ${chave in edicoes ? 'editor-pdf__item--editado' : ''}`}
                        style={estilo}
                        title={texto}
                        onClick={() => setItemEmEdicao(chave)}
                      />
                    );
                  })}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {totalEdicoes > 0 && <p className="sucesso">{totalEdicoes} trecho(s) editado(s).</p>}
      {erroSalvar && <p className="erro">{erroSalvar}</p>}

      <div className="acoes-lado-a-lado">
        <button type="button" onClick={salvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar edições'}
        </button>
        <button type="button" className="botao-secundario" onClick={onCancelar} disabled={salvando}>
          Cancelar
        </button>
      </div>
    </div>
  );
}
