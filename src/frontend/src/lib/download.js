/** Dispara o download de um blob no navegador, sem precisar de um <a href> apontando pra URL da API (útil quando o download exige o header Authorization). */
export function baixarBlob(blob, nomeArquivo) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nomeArquivo;
  link.click();
  URL.revokeObjectURL(url);
}
