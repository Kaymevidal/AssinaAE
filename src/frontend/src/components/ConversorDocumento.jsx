import { useState } from 'react';
import { converterParaDocx, converterParaPdf } from '../services/api';
import { baixarBlob } from '../lib/download';

function nomeSemExtensao(nome) {
  return nome.replace(/\.[^.]+$/, '');
}

function detectarExtensao(nome) {
  const minusculo = nome.toLowerCase();
  if (minusculo.endsWith('.pdf')) return 'pdf';
  if (minusculo.endsWith('.docx')) return 'docx';
  return null;
}

// Utilitário avulso: não cria contrato. Quando o resultado é um PDF, `onEnviarComoContrato` leva o arquivo pra aba de novo contrato.
export default function ConversorDocumento({ onEnviarComoContrato }) {
  const [arquivo, setArquivo] = useState(null);
  const [convertendo, setConvertendo] = useState(false);
  const [erro, setErro] = useState('');
  const [resultado, setResultado] = useState(null);

  async function converter(evento) {
    evento.preventDefault();
    if (!arquivo) {
      setErro('Selecione um arquivo PDF ou Word');
      return;
    }
    const extensao = detectarExtensao(arquivo.name);
    if (!extensao) {
      setErro('Selecione um arquivo .pdf ou .docx');
      return;
    }

    setConvertendo(true);
    setErro('');
    setResultado(null);
    try {
      const nomeBase = nomeSemExtensao(arquivo.name);
      if (extensao === 'pdf') {
        const blob = await converterParaDocx(arquivo);
        setResultado({ blob, tipo: 'docx', nomeArquivo: `${nomeBase}.docx` });
      } else {
        const blob = await converterParaPdf(arquivo);
        setResultado({ blob, tipo: 'pdf', nomeArquivo: `${nomeBase}.pdf` });
      }
    } catch (e) {
      setErro(e.response?.data?.erro || 'Não foi possível converter o arquivo');
    } finally {
      setConvertendo(false);
    }
  }

  function enviarComoContrato() {
    const arquivoPdf = new File([resultado.blob], resultado.nomeArquivo, { type: 'application/pdf' });
    onEnviarComoContrato(arquivoPdf);
  }

  return (
    <div>
      <p>Converta um contrato entre PDF e Word — escolha um arquivo e a conversão é feita automaticamente pro outro formato.</p>
      <form onSubmit={converter} className="form">
        <label>
          Arquivo (PDF ou Word)
          <input
            type="file"
            accept=".pdf,.docx"
            onChange={(e) => {
              setArquivo(e.target.files[0]);
              setResultado(null);
              setErro('');
            }}
          />
        </label>

        {erro && <p className="erro">{erro}</p>}

        <button type="submit" disabled={convertendo}>
          {convertendo ? 'Convertendo...' : 'Converter'}
        </button>
      </form>

      {resultado && (
        <div className="editar-contrato">
          <p className="sucesso">Conversão concluída: {resultado.nomeArquivo}</p>
          <div className="editar-contrato__acoes">
            <button type="button" className="botao-secundario" onClick={() => baixarBlob(resultado.blob, resultado.nomeArquivo)}>
              Baixar {resultado.tipo === 'pdf' ? 'PDF' : 'Word'}
            </button>
            {resultado.tipo === 'pdf' && (
              <button type="button" onClick={enviarComoContrato}>
                Enviar novo contrato
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
