import { Route, Routes, Link, useNavigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import RotaProtegida from './components/RotaProtegida';
import Login from './pages/Login';
import Registrar from './pages/Registrar';
import EsqueciSenha from './pages/EsqueciSenha';
import RedefinirSenha from './pages/RedefinirSenha';
import VerificarEmail from './pages/VerificarEmail';
import PainelProfissional from './pages/PainelProfissional';
import DetalheContrato from './pages/DetalheContrato';
import CriarContrato from './pages/CriarContrato';
import AssinarContrato from './pages/AssinarContrato';
import DownloadContrato from './pages/DownloadContrato';

export default function App() {
  const { autenticado, profissional, sair } = useAuth();
  const navigate = useNavigate();

  function sairEVoltarParaLogin() {
    sair();
    navigate('/login');
  }

  return (
    <div className="app">
      <header className="app__header">
        <Link to="/" className="app__logo">AssinaAE</Link>
        {autenticado && (
          <div className="app__conta">
            <span>{profissional.nome}</span>
            <button type="button" onClick={sairEVoltarParaLogin}>Sair</button>
          </div>
        )}
      </header>

      <main className="app__main">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/registrar" element={<Registrar />} />
          <Route path="/esqueci-senha" element={<EsqueciSenha />} />
          <Route path="/redefinir-senha/:token" element={<RedefinirSenha />} />
          <Route path="/verificar-email/:token" element={<VerificarEmail />} />

          <Route path="/" element={<RotaProtegida><PainelProfissional /></RotaProtegida>} />
          <Route path="/novo-contrato" element={<RotaProtegida><CriarContrato /></RotaProtegida>} />
          <Route path="/contratos/:id" element={<RotaProtegida><DetalheContrato /></RotaProtegida>} />

          <Route path="/assinar/:token" element={<AssinarContrato />} />
          <Route path="/download/:token" element={<DownloadContrato />} />
        </Routes>
      </main>
    </div>
  );
}
