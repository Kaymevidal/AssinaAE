import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { baixarContratoAutenticado, buscarContratoPorId } from '../services/api';

export default function DetalheContrato() {
  const { id } = useParams();
  const [contrato, setContrato] = useState(null);
  const [erro, setErro] = useState('');
  const [baixando, setBaixando] = useState(false);

  useEffect(() => {
    buscarContratoPorId(id)
      .then(setContrato)
      .catch((e) => setErro(e.response?.data?.erro || 'Contrato não encontrado'));
  }, [id]);

  async function baixar() {
    setBaixando(true);
    try {
      const blob = await baixarContratoAutenticado(id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `contrato-${id}-assinado.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setErro('Não foi possível baixar o PDF');
    } finally {
      setBaixando(false);
    }
  }

  if (erro) return <div className="card erro">{erro}</div>;
  if (!contrato) return <div className="card">Carregando...</div>;

  return (
    <div className="card">
      <h2>{contrato.titulo}</h2>
      <p>{contrato.descricao}</p>
      <p>Cliente: <strong>{contrato.nomeCliente}</strong></p>
      <p>
        Status profissional: <strong>{contrato.statusProfissional}</strong> &middot;{' '}
        Status cliente: <strong>{contrato.statusCliente}</strong>
      </p>

      {contrato.ambosAssinaram ? (
        <button type="button" onClick={baixar} disabled={baixando}>
          {baixando ? 'Baixando...' : 'Baixar PDF assinado'}
        </button>
      ) : (
        <p className="painel__vazio">Aguardando assinatura de ambas as partes.</p>
      )}
    </div>
  );
}
