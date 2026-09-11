import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { assinarContrato, buscarContratoPorToken, buscarPdfPreviewPorToken, rejeitarContrato } from '../services/api';
import SignaturePad from '../components/SignaturePad';
import PosicionadorAssinatura, { caixaPadrao, ULTIMA_PAGINA_SENTINELA } from '../components/PosicionadorAssinatura';

export default function AssinarContrato() {
  const { token } = useParams();
  const navigate = useNavigate();

  const [contrato, setContrato] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [assinatura, setAssinatura] = useState(null);
  const [pdfPreview, setPdfPreview] = useState(null);
  const [posicao, setPosicao] = useState(null);
  const [assinando, setAssinando] = useState(false);
  const [recusando, setRecusando] = useState(false);

  useEffect(() => {
    buscarContratoPorToken(token)
      .then(setContrato)
      .catch((e) => setErro(e.response?.data?.erro || 'Link de assinatura inválido ou expirado'))
      .finally(() => setCarregando(false));
  }, [token]);

  useEffect(() => {
    if (!contrato || contrato.statusPapel !== 'PENDENTE') return;
    buscarPdfPreviewPorToken(token)
      .then((bytes) => setPdfPreview(new Uint8Array(bytes)))
      .catch(() => {
        // Sem pré-visualização não dá pra posicionar visualmente, mas a assinatura
        // ainda pode ser confirmada numa posição padrão.
        setPdfPreview(null);
        setPosicao({ pagina: ULTIMA_PAGINA_SENTINELA, ...caixaPadrao(contrato.papel === 'CLIENTE') });
      });
  }, [contrato, token]);

  async function confirmarAssinatura() {
    if (!assinatura) {
      setErro('Desenhe sua assinatura antes de confirmar');
      return;
    }
    if (!posicao) {
      setErro('Aguarde o carregamento do PDF antes de confirmar');
      return;
    }

    setAssinando(true);
    setErro('');
    try {
      const atualizado = await assinarContrato(token, assinatura, posicao);
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

  async function confirmarRecusa() {
    if (!window.confirm('Tem certeza que quer recusar este contrato? Essa ação não pode ser desfeita.')) return;

    setRecusando(true);
    setErro('');
    try {
      const atualizado = await rejeitarContrato(token);
      setContrato(atualizado);
    } catch (e) {
      setErro(e.response?.data?.erro || 'Não foi possível recusar o contrato');
    } finally {
      setRecusando(false);
    }
  }

  if (carregando) return <div className="card">Carregando...</div>;
  if (erro && !contrato) return <div className="card erro">{erro}</div>;

  const jaAssinou = contrato.statusPapel === 'ASSINADO';
  const jaRecusou = contrato.statusPapel === 'REJEITADO';
  const nomeSignatario = contrato.papel === 'PROFISSIONAL' ? contrato.nomeProfissional : contrato.nomeCliente;

  return (
    <div className="card">
      <h2>{contrato.titulo}</h2>
      <p>{contrato.descricao}</p>
      <p>
        Profissional: <strong>{contrato.nomeProfissional}</strong> &middot; Cliente: <strong>{contrato.nomeCliente}</strong>
      </p>

      {jaRecusou ? (
        <p className="erro">Você recusou este contrato, {nomeSignatario}.</p>
      ) : jaAssinou ? (
        <p className="sucesso">Você já assinou este contrato, {nomeSignatario}. Aguardando a outra parte.</p>
      ) : (
        <>
          <p>Assine abaixo, {nomeSignatario}:</p>
          <SignaturePad onChange={setAssinatura} />

          {assinatura && pdfPreview && (
            <>
              <p>Agora posicione sua assinatura no documento:</p>
              <PosicionadorAssinatura
                pdfBytes={pdfPreview}
                assinaturaBase64={assinatura}
                ladoInicialDireito={contrato.papel === 'CLIENTE'}
                onChange={setPosicao}
              />
            </>
          )}

          {erro && <p className="erro">{erro}</p>}
          <div className="acoes-lado-a-lado">
            <button type="button" onClick={confirmarAssinatura} disabled={assinando || recusando}>
              {assinando ? 'Assinando...' : 'Confirmar assinatura'}
            </button>
            <button type="button" className="botao-perigo" onClick={confirmarRecusa} disabled={assinando || recusando}>
              {recusando ? 'Recusando...' : 'Recusar contrato'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
