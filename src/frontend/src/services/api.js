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

export function verificarEmail(token) {
  return api.get(`/auth/verificar-email/${token}`).then((res) => res.data);
}

export function reenviarVerificacao(email) {
  return api.post('/auth/reenviar-verificacao', { email }).then((res) => res.data);
}

export function esqueciSenha(email) {
  return api.post('/auth/esqueci-senha', { email }).then((res) => res.data);
}

export function redefinirSenha(token, novaSenha) {
  return api.post('/auth/redefinir-senha', { token, novaSenha }).then((res) => res.data);
}

// ---- Contratos ----

export function criarContrato({ titulo, descricao, emailCliente, nomeCliente, pdf }) {
  const formData = new FormData();
  const contrato = { titulo, descricao, emailCliente, nomeCliente };
  formData.append('contrato', new Blob([JSON.stringify(contrato)], { type: 'application/json' }));
  formData.append('pdf', pdf);

  return api.post('/contratos', formData).then((res) => res.data);
}

export function meusContratos(page = 0, size = 20) {
  return api.get('/contratos/meus', { params: { page, size } }).then((res) => res.data);
}

export function buscarContratoPorId(id) {
  return api.get(`/contratos/${id}`).then((res) => res.data);
}

export function excluirContrato(id) {
  return api.delete(`/contratos/${id}`);
}

export function buscarContratoPorToken(token) {
  return api.get(`/contratos/token/${token}`).then((res) => res.data);
}

/** Registra que quem acessou com aquele token visualizou o documento inteiro, pré-requisito pra assinar. */
export function confirmarLeituraContrato(token) {
  return api.post(`/contratos/token/${token}/confirmar-leitura`).then((res) => res.data);
}

export function assinarContrato(token, assinaturaBase64, posicao) {
  const { pagina, x, y, largura, altura } = posicao;
  return api
    .post(`/contratos/token/${token}/assinar`, { assinaturaBase64, pagina, x, y, largura, altura })
    .then((res) => res.data);
}

/** PDF no estado atual (ainda sem a assinatura de quem está acessando), para posicionar a assinatura antes de confirmar. */
export function buscarPdfPreviewPorToken(token) {
  return api
    .get(`/contratos/token/${token}/pdf-preview`, { responseType: 'arraybuffer' })
    .then((res) => res.data);
}

export function rejeitarContrato(token) {
  return api.post(`/contratos/token/${token}/rejeitar`).then((res) => res.data);
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

// ---- Conversão de documentos ----

function formDataComArquivo(arquivo) {
  const formData = new FormData();
  formData.append('arquivo', arquivo);
  return formData;
}

export function converterParaPdf(arquivo) {
  return api
    .post('/documentos/docx-para-pdf', formDataComArquivo(arquivo), { responseType: 'blob' })
    .then((res) => res.data);
}

export function converterParaDocx(arquivo) {
  return api
    .post('/documentos/pdf-para-docx', formDataComArquivo(arquivo), { responseType: 'blob' })
    .then((res) => res.data);
}

export function editarPdf(pdf, edicoes) {
  const formData = new FormData();
  formData.append('pdf', pdf);
  formData.append('edicoes', new Blob([JSON.stringify(edicoes)], { type: 'application/json' }));

  return api
    .post('/documentos/editar-pdf', formData, { responseType: 'blob' })
    .then((res) => res.data);
}

export default api;
