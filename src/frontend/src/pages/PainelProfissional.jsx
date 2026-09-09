import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { meusContratos, reenviarVerificacao } from '../services/api';
import { useAuth } from '../context/AuthContext';

function ItemContrato({ contrato }) {
  return (
    <li className="painel__item">
      <div>
        <strong>{contrato.titulo}</strong>
        <span className="painel__cliente"> · cliente: {contrato.nomeCliente}</span>
      </div>
      <Link to={`/contratos/${contrato.id}`}>Ver detalhes</Link>
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

export default function PainelProfissional() {
  const { profissional } = useAuth();
  const [pagina, setPagina] = useState(null);
  const [numeroPagina, setNumeroPagina] = useState(0);
  const [erro, setErro] = useState('');

  useEffect(() => {
    meusContratos(numeroPagina)
      .then(setPagina)
      .catch(() => setErro('Não foi possível carregar seus contratos'));
  }, [numeroPagina]);

  if (erro) return <div className="card erro">{erro}</div>;
  if (!pagina) return <div className="card">Carregando...</div>;

  const contratos = pagina.content;
  const recusado = (c) => c.statusProfissional === 'REJEITADO' || c.statusCliente === 'REJEITADO';
  const recusados = contratos.filter(recusado);
  const assinados = contratos.filter((c) => !recusado(c) && c.ambosAssinaram);
  const emAndamento = contratos.filter((c) => !recusado(c) && !c.ambosAssinaram);

  return (
    <div className="card">
      <div className="painel__cabecalho">
        <h2>Meus contratos</h2>
        <Link to="/novo-contrato">+ Novo contrato</Link>
      </div>

      {!profissional.emailVerificado && <AvisoEmailNaoVerificado email={profissional.email} />}

      <h3>Em andamento ({emAndamento.length})</h3>
      {emAndamento.length === 0 ? (
        <p className="painel__vazio">Nenhum contrato em andamento.</p>
      ) : (
        <ul className="painel__lista">
          {emAndamento.map((c) => <ItemContrato key={c.id} contrato={c} />)}
        </ul>
      )}

      <h3>Assinados ({assinados.length})</h3>
      {assinados.length === 0 ? (
        <p className="painel__vazio">Nenhum contrato assinado ainda.</p>
      ) : (
        <ul className="painel__lista">
          {assinados.map((c) => <ItemContrato key={c.id} contrato={c} />)}
        </ul>
      )}

      <h3>Recusados ({recusados.length})</h3>
      {recusados.length === 0 ? (
        <p className="painel__vazio">Nenhum contrato recusado.</p>
      ) : (
        <ul className="painel__lista">
          {recusados.map((c) => <ItemContrato key={c.id} contrato={c} />)}
        </ul>
      )}

      {pagina.totalPages > 1 && (
        <div className="painel__paginacao">
          <button type="button" disabled={pagina.first} onClick={() => setNumeroPagina((p) => p - 1)}>Anterior</button>
          <span>Página {pagina.number + 1} de {pagina.totalPages}</span>
          <button type="button" disabled={pagina.last} onClick={() => setNumeroPagina((p) => p + 1)}>Próxima</button>
        </div>
      )}
    </div>
  );
}
