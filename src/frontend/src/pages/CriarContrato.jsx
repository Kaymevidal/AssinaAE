import { useState } from 'react';
import ConversorDocumento from '../components/ConversorDocumento';
import NovoContratoForm from '../components/NovoContratoForm';

export default function CriarContrato() {
  const [aba, setAba] = useState('novo-contrato');
  const [pdf, setPdf] = useState(null);
  const [pdfVeioDoConversor, setPdfVeioDoConversor] = useState(false);

  function receberDoConversor(arquivo) {
    setPdf(arquivo);
    setPdfVeioDoConversor(true);
    setAba('novo-contrato');
  }

  function atualizarPdf(arquivo) {
    setPdf(arquivo);
    setPdfVeioDoConversor(false);
  }

  return (
    <div className="card">
      <div className="abas-documento">
        <button
          type="button"
          className={`abas-documento__item ${aba === 'converter' ? 'abas-documento__item--ativa' : ''}`}
          onClick={() => setAba('converter')}
        >
          Converter
        </button>
        <span className="abas-documento__separador">|</span>
        <button
          type="button"
          className={`abas-documento__item ${aba === 'novo-contrato' ? 'abas-documento__item--ativa' : ''}`}
          onClick={() => setAba('novo-contrato')}
        >
          Novo contrato
        </button>
      </div>

      {aba === 'converter' ? (
        <ConversorDocumento onEnviarComoContrato={receberDoConversor} />
      ) : (
        <NovoContratoForm pdf={pdf} setPdf={atualizarPdf} pdfAnexadoAutomaticamente={pdfVeioDoConversor} />
      )}
    </div>
  );
}
