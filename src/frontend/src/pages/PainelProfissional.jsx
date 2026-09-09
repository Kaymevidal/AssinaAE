import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { meusContratos } from '../services/api';

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

export default function PainelProfissional() {
  const [contratos, setContratos] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    meusContratos()
      .then(setContratos)
      .catch(() => setErro('Não foi possível carregar seus contratos'));
  }, []);

  if (erro) return <div className="card erro">{erro}</div>;
  if (!contratos) return <div className="card">Carregando...</div>;

  const emAndamento = contratos.filter((c) => !c.ambosAssinaram);
  const assinados = contratos.filter((c) => c.ambosAssinaram);

  return (
    <div className="card">
      <div className="painel__cabecalho">
        <h2>Meus contratos</h2>
        <Link to="/novo-contrato">+ Novo contrato</Link>
      </div>

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
    </div>
  );
}
