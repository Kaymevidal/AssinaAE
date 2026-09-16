import { useState } from 'react';
import { converterParaPdf } from '../services/api';
import { baixarBlob } from '../lib/download';

function nomeSemExtensao(nome) {
  return nome.replace(/\.[^.]+$/, '');
}

// Utilitário avulso: converte Word pra PDF. Não cria contrato — `onEnviarComoContrato` leva o resultado pra aba de novo contrato.
export default function ConversorDocumento({ onEnviarComoContrato }) {
  const [arquivo, setArquivo] = useState(null);
  const [convertendo, setConvertendo] = useState(false);
  const [erro, setErro] = useState('');
  const [resultado, setResultado] = useState(null);

  async function converter(evento) {
    evento.preventDefault();
    if (!arquivo) {
      setErro('Selecione um arquivo Word (.docx)');
      return;
    }

    setConvertendo(true);
    setErro('');
    setResultado(null);
    try {
      const blob = await converterParaPdf(arquivo);
      setResultado({ blob, nomeArquivo: `${nomeSemExtensao(arquivo.name)}.pdf` });
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
      <p>Converta um contrato do Word para PDF.</p>
      <form onSubmit={converter} className="form">
        <label>
          Arquivo Word (.docx)
          <input
            type="file"
            accept=".docx"
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
              Baixar PDF
            </button>
            <button type="button" onClick={enviarComoContrato}>
              Enviar novo contrato
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
