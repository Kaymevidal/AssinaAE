import { useState } from 'react';
import { Link } from 'react-router-dom';
import { esqueciSenha } from '../services/api';

export default function EsqueciSenha() {
  const [email, setEmail] = useState('');
  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);

  async function enviar(evento) {
    evento.preventDefault();
    setEnviando(true);
    try {
      await esqueciSenha(email);
    } finally {
      // Sempre mostra a mesma mensagem, exista ou não a conta — evita que alguém
      // use este formulário pra descobrir quais emails estão cadastrados.
      setEnviando(false);
      setEnviado(true);
    }
  }

  if (enviado) {
    return (
      <div className="card">
        <h2>Verifique seu email</h2>
        <p>Se existir uma conta com esse email, enviamos um link para redefinir a senha.</p>
        <Link to="/login">Voltar para o login</Link>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Esqueci minha senha</h2>
      <form onSubmit={enviar} className="form">
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <button type="submit" disabled={enviando}>{enviando ? 'Enviando...' : 'Enviar link de redefinição'}</button>
      </form>
      <p className="rodape-form">
        <Link to="/login">Voltar para o login</Link>
      </p>
    </div>
  );
}
