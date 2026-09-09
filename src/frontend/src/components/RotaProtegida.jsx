import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RotaProtegida({ children }) {
  const { autenticado } = useAuth();
  return autenticado ? children : <Navigate to="/login" replace />;
}
