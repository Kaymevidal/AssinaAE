import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { buscarContratoPorToken, urlDownloadPorToken } from '../services/api';

export default function DownloadContrato() {
  const { token } = useParams();
  const [contrato, setContrato] = useState(null);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    buscarContratoPorToken(token)
      .then(setContrato)
      .catch((e) => setErro(e.response?.data?.erro || 'Link inválido ou expirado'))
      .finally(() => setCarregando(false));
  }, [token]);

  if (carregando) return <div className="card">Carregando...</div>;
  if (erro) return <div className="card erro">{erro}</div>;

  return (
    <div className="card">
      <h2>{contrato.titulo}</h2>
      {contrato.ambosAssinaram ? (
        <>
          <p className="sucesso">Contrato assinado por ambas as partes.</p>
          <a href={urlDownloadPorToken(token)} className="botao-link">Baixar PDF assinado</a>
        </>
      ) : (
        <p>Aguardando a assinatura da outra parte para liberar o download.</p>
      )}
    </div>
  );
}
