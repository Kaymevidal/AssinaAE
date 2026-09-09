import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { verificarEmail } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function VerificarEmail() {
  const { token } = useParams();
  const { entrarComSessao } = useAuth();
  const [estado, setEstado] = useState('carregando'); // carregando | sucesso | erro

  useEffect(() => {
    verificarEmail(token)
      .then((resposta) => {
        entrarComSessao(resposta);
        setEstado('sucesso');
      })
      .catch(() => setEstado('erro'));
  }, [token, entrarComSessao]);

  if (estado === 'carregando') return <div className="card">Confirmando seu email...</div>;

  if (estado === 'erro') {
    return (
      <div className="card">
        <h2>Link inválido</h2>
        <p className="erro">Esse link de verificação não é mais válido.</p>
        <Link to="/login">Voltar para o login</Link>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Email confirmado!</h2>
      <p className="sucesso">Agora você já pode enviar contratos.</p>
      <Link to="/">Ir para o painel</Link>
    </div>
  );
}
