import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { redefinirSenha } from '../services/api';

export default function RedefinirSenha() {
  const { token } = useParams();
  const navigate = useNavigate();
  const [novaSenha, setNovaSenha] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [sucesso, setSucesso] = useState(false);

  async function enviar(evento) {
    evento.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      await redefinirSenha(token, novaSenha);
      setSucesso(true);
      setTimeout(() => navigate('/login'), 2000);
    } catch (e) {
      setErro(e.response?.data?.erro || 'Link inválido ou expirado');
    } finally {
      setEnviando(false);
    }
  }

  if (sucesso) {
    return (
      <div className="card">
        <h2>Senha redefinida!</h2>
        <p className="sucesso">Já pode entrar com a nova senha. Redirecionando...</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Redefinir senha</h2>
      <form onSubmit={enviar} className="form">
        <label>
          Nova senha
          <input type="password" minLength={8} value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} required />
        </label>
        {erro && <p className="erro">{erro}</p>}
        <button type="submit" disabled={enviando}>{enviando ? 'Salvando...' : 'Redefinir senha'}</button>
      </form>
      <p className="rodape-form">
        <Link to="/login">Voltar para o login</Link>
      </p>
    </div>
  );
}
