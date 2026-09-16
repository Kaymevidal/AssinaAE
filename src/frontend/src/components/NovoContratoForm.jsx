import { useState } from 'react';
import { Link } from 'react-router-dom';
import { criarContrato } from '../services/api';
import EditorPdf from './EditorPdf';

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

  const [editorAberto, setEditorAberto] = useState(false);
  const [pdfBytesEditor, setPdfBytesEditor] = useState(null);

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

  async function abrirEditor() {
    const buffer = await pdf.arrayBuffer();
    setPdfBytesEditor(new Uint8Array(buffer));
    setEditorAberto(true);
  }

  function fecharEditor(pdfEditadoBlob) {
    if (pdfEditadoBlob) {
      setPdf(new File([pdfEditadoBlob], 'contrato-editado.pdf', { type: 'application/pdf' }));
    }
    setEditorAberto(false);
    setPdfBytesEditor(null);
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
          <input type="file" accept="application/pdf" onChange={(e) => setPdf(e.target.files[0])} />
        </label>

        {/* sem "required" no input: um PDF anexado pela aba Converter ou pelo editor só existe no
            estado do React, nunca no input nativo — o navegador bloquearia o envio achando que está vazio */}
        {pdf && <p className="posicionador__dica">Arquivo atual: {pdf.name}</p>}
        {pdf && pdfAnexadoAutomaticamente && <p className="aviso">Contrato anexado automaticamente pela conversão.</p>}

        {pdf && !editorAberto && (
          <button type="button" className="botao-secundario" onClick={abrirEditor}>
            Editar PDF
          </button>
        )}

        {editorAberto && pdfBytesEditor && (
          <EditorPdf pdfBytes={pdfBytesEditor} onSalvar={fecharEditor} onCancelar={() => fecharEditor(null)} />
        )}

        {erro && <p className="erro">{erro}</p>}

        <button type="submit" disabled={enviando}>
          {enviando ? 'Enviando...' : 'Enviar para assinatura'}
        </button>
      </form>
    </>
  );
}
