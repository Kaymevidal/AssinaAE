import { Route, Routes, Link } from 'react-router-dom';
import CriarContrato from './pages/CriarContrato';
import AssinarContrato from './pages/AssinarContrato';
import DownloadContrato from './pages/DownloadContrato';

export default function App() {
  return (
    <div className="app">
      <header className="app__header">
        <Link to="/" className="app__logo">Assinatura Digital</Link>
      </header>

      <main className="app__main">
        <Routes>
          <Route path="/" element={<CriarContrato />} />
          <Route path="/assinar/:token" element={<AssinarContrato />} />
          <Route path="/download/:id" element={<DownloadContrato />} />
        </Routes>
      </main>
    </div>
  );
}
