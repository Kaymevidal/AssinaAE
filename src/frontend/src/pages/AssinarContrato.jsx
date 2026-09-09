import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { assinarContrato, buscarContratoPorToken } from '../services/api';
import SignaturePad from '../components/SignaturePad';

export default function AssinarContrato() {
  const { token } = useParams();
  const navigate = useNavigate();

  const [contrato, setContrato] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [assinatura, setAssinatura] = useState(null);
  const [assinando, setAssinando] = useState(false);

  useEffect(() => {
    buscarContratoPorToken(token)
      .then(setContrato)
      .catch((e) => setErro(e.response?.data?.erro || 'Link de assinatura inválido ou expirado'))
      .finally(() => setCarregando(false));
  }, [token]);

  async function confirmarAssinatura() {
    if (!assinatura) {
      setErro('Desenhe sua assinatura antes de confirmar');
      return;
    }

    setAssinando(true);
    setErro('');
    try {
      const atualizado = await assinarContrato(token, assinatura);
      setContrato(atualizado);
      if (atualizado.ambosAssinaram) {
        navigate(`/download/${token}`);
      }
    } catch (e) {
      setErro(e.response?.data?.erro || 'Não foi possível registrar a assinatura');
    } finally {
      setAssinando(false);
    }
  }

  if (carregando) return <div className="card">Carregando...</div>;
  if (erro && !contrato) return <div className="card erro">{erro}</div>;

  const jaAssinou = contrato.statusPapel === 'ASSINADO';
  const nomeSignatario = contrato.papel === 'PROFISSIONAL' ? contrato.nomeProfissional : contrato.nomeCliente;

  return (
    <div className="card">
      <h2>{contrato.titulo}</h2>
      <p>{contrato.descricao}</p>
      <p>
        Profissional: <strong>{contrato.nomeProfissional}</strong> &middot; Cliente: <strong>{contrato.nomeCliente}</strong>
      </p>

      {jaAssinou ? (
        <p className="sucesso">Você já assinou este contrato, {nomeSignatario}. Aguardando a outra parte.</p>
      ) : (
        <>
          <p>Assine abaixo, {nomeSignatario}:</p>
          <SignaturePad onChange={setAssinatura} />
          {erro && <p className="erro">{erro}</p>}
          <button type="button" onClick={confirmarAssinatura} disabled={assinando}>
            {assinando ? 'Assinando...' : 'Confirmar assinatura'}
          </button>
        </>
      )}
    </div>
  );
}
