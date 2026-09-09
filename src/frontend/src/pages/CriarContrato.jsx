import { useState } from 'react';
import { Link } from 'react-router-dom';
import { criarContrato } from '../services/api';

const CAMPOS_INICIAIS = {
  titulo: '',
  descricao: '',
  nomeCliente: '',
  emailCliente: '',
};

export default function CriarContrato() {
  const [campos, setCampos] = useState(CAMPOS_INICIAIS);
  const [pdf, setPdf] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState(false);

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

  if (sucesso) {
    return (
      <div className="card">
        <h2>Contrato enviado!</h2>
        <p>Os links de assinatura foram enviados por email para você e para o cliente.</p>
        <div className="acoes-lado-a-lado">
          <button type="button" onClick={() => setSucesso(false)}>Criar outro contrato</button>
          <Link to="/">Ver meus contratos</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
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

        {erro && <p className="erro">{erro}</p>}

        <button type="submit" disabled={enviando}>
          {enviando ? 'Enviando...' : 'Enviar para assinatura'}
        </button>
      </form>
    </div>
  );
}
