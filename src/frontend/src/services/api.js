import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
});

export function criarContrato({ titulo, descricao, emailProfissional, emailCliente, nomeProfissional, nomeCliente, pdf }) {
  const formData = new FormData();
  const contrato = { titulo, descricao, emailProfissional, emailCliente, nomeProfissional, nomeCliente };
  formData.append('contrato', new Blob([JSON.stringify(contrato)], { type: 'application/json' }));
  formData.append('pdf', pdf);

  return api.post('/contratos', formData).then((res) => res.data);
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

export default api;
