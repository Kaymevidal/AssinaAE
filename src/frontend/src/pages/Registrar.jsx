import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import BotaoGoogle from '../components/BotaoGoogle';
import { reenviarVerificacao } from '../services/api';

export default function Registrar() {
  const { registrar, entrarComGoogle } = useAuth();
  const navigate = useNavigate();

  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [registrado, setRegistrado] = useState(false);
  const [reenviado, setReenviado] = useState(false);

  async function submeter(evento) {
    evento.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      await registrar({ nome, email, senha });
      setRegistrado(true);
    } catch (e) {
      setErro(e.response?.data?.erro || 'Não foi possível criar a conta');
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

  async function reenviar() {
    await reenviarVerificacao(email);
    setReenviado(true);
  }

  if (registrado) {
    return (
      <div className="card">
        <h2>Confirme seu email</h2>
        <p>Enviamos um link de confirmação para <strong>{email}</strong>. Você já pode entrar, mas só consegue enviar contratos depois de confirmar.</p>
        {reenviado ? (
          <p className="sucesso">Email reenviado.</p>
        ) : (
          <button type="button" onClick={reenviar}>Reenviar email</button>
        )}
        <p className="rodape-form">
          <Link to="/login">Ir para o login</Link>
        </p>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Criar conta de profissional</h2>
      <form onSubmit={submeter} className="form">
        <label>
          Nome
          <input value={nome} onChange={(e) => setNome(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Senha
          <input type="password" minLength={8} value={senha} onChange={(e) => setSenha(e.target.value)} required />
        </label>

        {erro && <p className="erro">{erro}</p>}

        <button type="submit" disabled={enviando}>{enviando ? 'Criando conta...' : 'Criar conta'}</button>
      </form>

      <div className="separador">ou</div>
      <BotaoGoogle onCredential={submeterGoogle} />

      <p className="rodape-form">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </div>
  );
}
