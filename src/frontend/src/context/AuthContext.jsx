import { createContext, useContext, useState } from 'react';
import * as api from '../services/api';

const AuthContext = createContext(null);

function lerProfissionalSalvo() {
  try {
    const bruto = localStorage.getItem('assinatura_digital_profissional');
    return bruto ? JSON.parse(bruto) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [profissional, setProfissional] = useState(lerProfissionalSalvo);

  function salvarSessao(resposta) {
    const { token, ...dadosProfissional } = resposta;
    localStorage.setItem(api.TOKEN_KEY, token);
    localStorage.setItem('assinatura_digital_profissional', JSON.stringify(dadosProfissional));
    setProfissional(dadosProfissional);
  }

  async function entrar({ email, senha }) {
    salvarSessao(await api.login({ email, senha }));
  }

  async function registrar({ nome, email, senha }) {
    salvarSessao(await api.registrar({ nome, email, senha }));
  }

  async function entrarComGoogle(credential) {
    salvarSessao(await api.loginGoogle(credential));
  }

  function sair() {
    localStorage.removeItem(api.TOKEN_KEY);
    localStorage.removeItem('assinatura_digital_profissional');
    setProfissional(null);
  }

  return (
    <AuthContext.Provider value={{ profissional, autenticado: !!profissional, entrar, registrar, entrarComGoogle, sair }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const contexto = useContext(AuthContext);
  if (!contexto) throw new Error('useAuth precisa estar dentro de um AuthProvider');
  return contexto;
}
