import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { excluirContrato, meusContratos, reenviarVerificacao } from '../services/api';
import { useAuth } from '../context/AuthContext';

function IconeLixeira() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
      <line x1="10" y1="11" x2="10" y2="17" />
      <line x1="14" y1="11" x2="14" y2="17" />
    </svg>
  );
}

function ItemContrato({ contrato, onExcluir }) {
  const [excluindo, setExcluindo] = useState(false);

  async function excluir() {
    if (!window.confirm(`Tem certeza que quer excluir o contrato "${contrato.titulo}"? Essa ação não pode ser desfeita.`)) return;

    setExcluindo(true);
    try {
      await onExcluir(contrato.id);
    } catch {
      setExcluindo(false);
      window.alert('Não foi possível excluir o contrato. Tente novamente.');
    }
  }

  return (
    <li className="painel__item">
      <div>
        <strong>{contrato.titulo}</strong>
        <span className="painel__cliente"> · cliente: {contrato.nomeCliente}</span>
      </div>
      <div className="painel__item__acoes">
        <Link to={`/contratos/${contrato.id}`}>Ver detalhes</Link>
        <button
          type="button"
          className="painel__excluir"
          onClick={excluir}
          disabled={excluindo}
          aria-label="Excluir contrato"
          title="Excluir contrato"
        >
          <IconeLixeira />
        </button>
      </div>
    </li>
  );
}

function AvisoEmailNaoVerificado({ email }) {
  const [reenviado, setReenviado] = useState(false);

  async function reenviar() {
    await reenviarVerificacao(email);
    setReenviado(true);
  }

  return (
    <div className="aviso">
      {reenviado ? (
        <span>Email reenviado — confira sua caixa de entrada.</span>
      ) : (
        <>
          <span>Confirme seu email para poder enviar contratos.</span>{' '}
          <button type="button" onClick={reenviar}>Reenviar email de confirmação</button>
        </>
      )}
    </div>
  );
}

export default function MeusContratos() {
  const { profissional } = useAuth();
  const [pagina, setPagina] = useState(null);
  const [numeroPagina, setNumeroPagina] = useState(0);
  const [erro, setErro] = useState('');

  useEffect(() => {
    meusContratos(numeroPagina)
      .then(setPagina)
      .catch(() => setErro('Não foi possível carregar seus contratos'));
  }, [numeroPagina]);

  async function removerContrato(id) {
    await excluirContrato(id);
    setPagina((atual) => ({ ...atual, content: atual.content.filter((c) => c.id !== id) }));
  }

  if (erro) return <p className="erro">{erro}</p>;
  if (!pagina) return <p>Carregando...</p>;

  const contratos = pagina.content;
  const recusado = (c) => c.statusProfissional === 'REJEITADO' || c.statusCliente === 'REJEITADO';
  const recusados = contratos.filter(recusado);
  const assinados = contratos.filter((c) => !recusado(c) && c.ambosAssinaram);
  const aguardandoAssinatura = contratos.filter((c) => !recusado(c) && !c.ambosAssinaram);

  return (
    <>
      <h2>Meus contratos</h2>

      {!profissional.emailVerificado && <AvisoEmailNaoVerificado email={profissional.email} />}

      <h3>Aguardando assinatura ({aguardandoAssinatura.length})</h3>
      {aguardandoAssinatura.length === 0 ? (
        <p className="painel__vazio">Nenhum contrato aguardando assinatura.</p>
      ) : (
        <ul className="painel__lista">
          {aguardandoAssinatura.map((c) => <ItemContrato key={c.id} contrato={c} onExcluir={removerContrato} />)}
        </ul>
      )}

      <h3>Assinados ({assinados.length})</h3>
      {assinados.length === 0 ? (
        <p className="painel__vazio">Nenhum contrato assinado ainda.</p>
      ) : (
        <ul className="painel__lista">
          {assinados.map((c) => <ItemContrato key={c.id} contrato={c} onExcluir={removerContrato} />)}
        </ul>
      )}

      <h3>Recusados ({recusados.length})</h3>
      {recusados.length === 0 ? (
        <p className="painel__vazio">Nenhum contrato recusado.</p>
      ) : (
        <ul className="painel__lista">
          {recusados.map((c) => <ItemContrato key={c.id} contrato={c} onExcluir={removerContrato} />)}
        </ul>
      )}

      {pagina.totalPages > 1 && (
        <div className="painel__paginacao">
          <button type="button" disabled={pagina.first} onClick={() => setNumeroPagina((p) => p - 1)}>Anterior</button>
          <span>Página {pagina.number + 1} de {pagina.totalPages}</span>
          <button type="button" disabled={pagina.last} onClick={() => setNumeroPagina((p) => p + 1)}>Próxima</button>
        </div>
      )}
    </>
  );
}
