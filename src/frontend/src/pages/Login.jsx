import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import BotaoGoogle from '../components/BotaoGoogle';
import Logo from '../components/Logo';

export default function Login() {
  const { entrar, entrarComGoogle } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function submeter(evento) {
    evento.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      await entrar({ email, senha });
      navigate('/');
    } catch (e) {
      setErro(e.response?.data?.erro || 'Não foi possível entrar');
    } finally {
      setEnviando(false);
    }
  }

  async function submeterGoogle(credential) {
    setErro('');
    try {
      await entrarComGoogle(credential);
      navigate('/');
    } catch (e) {
      setErro(e.response?.data?.erro || 'Não foi possível entrar com o Google');
    }
  }

  return (
    <div className="card">
      <Logo className="login__logo" />
      <h2>Entrar</h2>
      <form onSubmit={submeter} className="form">
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Senha
          <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
        </label>
        <Link to="/esqueci-senha" className="link-discreto">Esqueci minha senha</Link>

        {erro && <p className="erro">{erro}</p>}

        <button type="submit" disabled={enviando}>{enviando ? 'Entrando...' : 'Entrar'}</button>
      </form>

      <div className="separador">ou</div>
      <BotaoGoogle onCredential={submeterGoogle} />

      <p className="rodape-form">
        Não tem conta? <Link to="/registrar">Registre-se</Link>
      </p>
    </div>
  );
}
