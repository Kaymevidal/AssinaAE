import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { buscarContratoPorId, urlDownload } from '../services/api';

export default function DownloadContrato() {
  const { id } = useParams();
  const [contrato, setContrato] = useState(null);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    buscarContratoPorId(id)
      .then(setContrato)
      .catch((e) => setErro(e.response?.data?.erro || 'Contrato não encontrado'))
      .finally(() => setCarregando(false));
  }, [id]);

  if (carregando) return <div className="card">Carregando...</div>;
  if (erro) return <div className="card erro">{erro}</div>;

  return (
    <div className="card">
      <h2>{contrato.titulo}</h2>
      {contrato.ambosAssinaram ? (
        <>
          <p className="sucesso">Contrato assinado por ambas as partes.</p>
          <a href={urlDownload(id)} className="botao-link">Baixar PDF assinado</a>
        </>
      ) : (
        <p>
          Aguardando assinatura de{' '}
          {contrato.statusProfissional !== 'ASSINADO' && contrato.nomeProfissional}
          {contrato.statusProfissional !== 'ASSINADO' && contrato.statusCliente !== 'ASSINADO' && ' e '}
          {contrato.statusCliente !== 'ASSINADO' && contrato.nomeCliente}.
        </p>
      )}
    </div>
  );
}
