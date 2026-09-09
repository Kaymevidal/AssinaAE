import axios from 'axios';

export const TOKEN_KEY = 'assinatura_digital_token';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// ---- Autenticação ----

export function registrar({ nome, email, senha }) {
  return api.post('/auth/registrar', { nome, email, senha }).then((res) => res.data);
}

export function login({ email, senha }) {
  return api.post('/auth/login', { email, senha }).then((res) => res.data);
}

export function loginGoogle(credential) {
  return api.post('/auth/google', { credential }).then((res) => res.data);
}

// ---- Contratos ----

export function criarContrato({ titulo, descricao, emailCliente, nomeCliente, pdf }) {
  const formData = new FormData();
  const contrato = { titulo, descricao, emailCliente, nomeCliente };
  formData.append('contrato', new Blob([JSON.stringify(contrato)], { type: 'application/json' }));
  formData.append('pdf', pdf);

  return api.post('/contratos', formData).then((res) => res.data);
}

export function meusContratos() {
  return api.get('/contratos/meus').then((res) => res.data);
}

export function buscarContratoPorId(id) {
  return api.get(`/contratos/${id}`).then((res) => res.data);
}

export function buscarContratoPorToken(token) {
  return api.get(`/contratos/token/${token}`).then((res) => res.data);
}

export function assinarContrato(token, assinaturaBase64) {
  return api.post(`/contratos/token/${token}/assinar`, { assinaturaBase64 }).then((res) => res.data);
}

export function urlDownload(id) {
  return `${api.defaults.baseURL}/contratos/${id}/download`;
}

export function urlDownloadPorToken(token) {
  return `${api.defaults.baseURL}/contratos/token/${token}/download`;
}

/** Download autenticado (painel do profissional) — precisa do header Authorization, por isso não dá pra usar <a href> puro. */
export function baixarContratoAutenticado(id) {
  return api.get(`/contratos/${id}/download`, { responseType: 'blob' }).then((res) => res.data);
}

export default api;
