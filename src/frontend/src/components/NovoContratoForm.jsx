import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { converterParaDocx, converterParaPdf, criarContrato } from '../services/api';
import { baixarBlob } from '../lib/download';
import VisualizadorPdf from './VisualizadorPdf';

const CAMPOS_INICIAIS = {
  titulo: '',
  descricao: '',
  nomeCliente: '',
  emailCliente: '',
};

// `pdf`/`setPdf` vêm de fora (do container de abas) pra sobreviver à troca de aba
export default function NovoContratoForm({ pdf, setPdf, pdfAnexadoAutomaticamente }) {
  const [campos, setCampos] = useState(CAMPOS_INICIAIS);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState(false);

  const [editando, setEditando] = useState(false);
  const [erroEdicao, setErroEdicao] = useState('');
  const [mostrarPreview, setMostrarPreview] = useState(false);
  const [previewBytes, setPreviewBytes] = useState(null);

  useEffect(() => {
    setMostrarPreview(false);
  }, [pdf]);

  function atualizarCampo(evento) {
    const { name, value } = evento.target;
    setCampos((atual) => ({ ...atual, [name]: value }));
  }

  async function enviar(evento) {
    evento.preventDefault();
    setErro('');

    if (!pdf) {
      setErro('Selecione o PDF do contrato');
      return;
    }

    setEnviando(true);
    try {
      await criarContrato({ ...campos, pdf });
      setSucesso(true);
      setCampos(CAMPOS_INICIAIS);
      setPdf(null);
    } catch (e) {
      setErro(e.response?.data?.erro || 'Não foi possível criar o contrato');
    } finally {
      setEnviando(false);
    }
  }

  async function baixarComoWord() {
    setEditando('baixando');
    setErroEdicao('');
    try {
      const blob = await converterParaDocx(pdf);
      baixarBlob(blob, 'contrato-para-editar.docx');
    } catch (e) {
      setErroEdicao(e.response?.data?.erro || 'Não foi possível gerar o Word');
    } finally {
      setEditando(false);
    }
  }

  async function enviarArquivoEditado(evento) {
    const arquivoDocx = evento.target.files[0];
    evento.target.value = '';
    if (!arquivoDocx) return;

    setEditando('convertendo');
    setErroEdicao('');
    try {
      const blob = await converterParaPdf(arquivoDocx);
      setPdf(new File([blob], 'contrato-editado.pdf', { type: 'application/pdf' }));
    } catch (e) {
      setErroEdicao(e.response?.data?.erro || 'Não foi possível converter o arquivo editado');
    } finally {
      setEditando(false);
    }
  }

  async function alternarPreview() {
    if (mostrarPreview) {
      setMostrarPreview(false);
      return;
    }
    const buffer = await pdf.arrayBuffer();
    setPreviewBytes(new Uint8Array(buffer));
    setMostrarPreview(true);
  }

  if (sucesso) {
    return (
      <>
        <h2>Contrato enviado!</h2>
        <p>Os links de assinatura foram enviados por email para você e para o cliente.</p>
        <div className="acoes-lado-a-lado">
          <button type="button" onClick={() => setSucesso(false)}>Criar outro contrato</button>
          <Link to="/">Ver meus contratos</Link>
        </div>
      </>
    );
  }

  return (
    <>
      <h2>Novo contrato</h2>
      <form onSubmit={enviar} className="form">
        <label>
          Título
          <input name="titulo" value={campos.titulo} onChange={atualizarCampo} required />
        </label>

        <label>
          Descrição
          <textarea name="descricao" value={campos.descricao} onChange={atualizarCampo} required />
        </label>

        <div className="form__grid">
          <label>
            Nome do cliente
            <input name="nomeCliente" value={campos.nomeCliente} onChange={atualizarCampo} required />
          </label>
          <label>
            Email do cliente
            <input type="email" name="emailCliente" value={campos.emailCliente} onChange={atualizarCampo} required />
          </label>
        </div>

        <label>
          PDF do contrato
          <input type="file" accept="application/pdf" onChange={(e) => setPdf(e.target.files[0])} required />
        </label>

        {pdf && pdfAnexadoAutomaticamente && <p className="aviso">Contrato anexado automaticamente pela conversão.</p>}

        {pdf && (
          <div className="editar-contrato">
            <p>Precisa preencher os dados do cliente num contrato-modelo? Edite no Word antes de enviar:</p>
            <div className="editar-contrato__acoes">
              <button type="button" className="botao-secundario" onClick={baixarComoWord} disabled={!!editando}>
                {editando === 'baixando' ? 'Convertendo...' : 'Baixar como Word'}
              </button>
              <label className="botao-secundario" style={{ display: 'inline-block', cursor: editando ? 'not-allowed' : 'pointer' }}>
                {editando === 'convertendo' ? 'Convertendo...' : 'Enviar arquivo editado (.docx)'}
                <input
                  type="file"
                  accept=".docx"
                  onChange={enviarArquivoEditado}
                  disabled={!!editando}
                  style={{ display: 'none' }}
                />
              </label>
              <button type="button" className="botao-secundario" onClick={alternarPreview}>
                {mostrarPreview ? 'Ocultar prévia' : 'Visualizar'}
              </button>
            </div>
            {erroEdicao && <p className="erro">{erroEdicao}</p>}
            {mostrarPreview && previewBytes && <VisualizadorPdf pdfBytes={previewBytes} />}
          </div>
        )}

        {erro && <p className="erro">{erro}</p>}

        <button type="submit" disabled={enviando}>
          {enviando ? 'Enviando...' : 'Enviar para assinatura'}
        </button>
      </form>
    </>
  );
}
