import { useState, useEffect } from 'react';
import axios from 'axios';
import { useAuth } from 'features/auth/context/AuthContext';
import { login as loginService } from 'features/auth/services/authService';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import logoImg from 'assets/ProveedoresLogo_Light.png';
import './LoginPage.css';


const LoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/');
      return;
    }

    // Si venimos de una sesión expirada (401/403), mostramos un mensaje claro.
    const sessionExpiredFlag = localStorage.getItem('sessionExpired');
    if (sessionExpiredFlag === '1') {
      toast.error('Tu sesión ha expirado. Vuelve a iniciar sesión.');
      localStorage.removeItem('sessionExpired');
    }
  }, [isAuthenticated, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError(null);
    setLoading(true);

    try {
      const authResponse = await loginService({ username, password });
      login(authResponse);
      toast.success(`Bienvenido, ${authResponse.username}`);
    } catch (error) {
      let errorMessage = 'No se pudo iniciar sesión. Inténtalo de nuevo en unos instantes.';

      if (axios.isAxiosError(error)) {
        const status = error.response?.status;
        const responseMessage = error.response?.data?.message;

        if (status === 401 || status === 403) {
          errorMessage = 'Usuario o contraseña incorrectos. Verifica tus datos.';
        } else if (typeof responseMessage === 'string' && responseMessage.trim().length > 0) {
          errorMessage = responseMessage;
        }
      }

      setLoginError(errorMessage);
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">

      {/* ── Panel de marca ── */}
      <div className="login-brand-panel">
        <div className="bubble bubble-1" />
        <div className="bubble bubble-2" />
        <div className="bubble bubble-3" />

        <div className="brand-content">
          <img src={logoImg} alt="Logo" className="brand-logo-img" />

          <div className="brand-text">
            <p className="brand-subtitle">Gestión Mayorista de Stock</p>
          </div>

          <div className="brand-divider" />

          <ul className="brand-features">
            <li className="brand-feature">
              <span className="brand-feature-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                  stroke="rgba(255,255,255,0.85)" strokeWidth="2">
                  <rect x="2" y="3" width="20" height="14" rx="2" />
                  <path d="M8 21h8M12 17v4" />
                </svg>
              </span>
              Control de inventario en tiempo real
            </li>
            <li className="brand-feature">
              <span className="brand-feature-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                  stroke="rgba(255,255,255,0.85)" strokeWidth="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
              </span>
              Gestión de proveedores y usuarios
            </li>
            <li className="brand-feature">
              <span className="brand-feature-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                  stroke="rgba(255,255,255,0.85)" strokeWidth="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                </svg>
              </span>
              Historial de precios y análisis
            </li>
          </ul>
        </div>
      </div>

      {/* ── Panel de formulario ── */}
      <div className="login-form-panel">
        <div className="login-form-wrapper">

          <div className="form-heading">
            <h2>Bienvenido</h2>
            <p>Inicia sesión para acceder al sistema</p>
          </div>

          {loginError && (
            <div className="login-error-banner" role="alert" aria-live="polite">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{loginError}</span>
            </div>
          )}

          <form className="login-form" onSubmit={handleSubmit}>

            <div className="form-group">
              <label htmlFor="username">Usuario</label>
              <div className="input-wrapper">
                <svg className="input-icon" width="18" height="18" viewBox="0 0 24 24"
                  fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <input
                  id="username"
                  type="text"
                  placeholder="usuario"
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    if (loginError) {
                      setLoginError(null);
                    }
                  }}
                  autoComplete="username"
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="password">Contraseña</label>
              <div className="input-wrapper">
                <svg className="input-icon" width="18" height="18" viewBox="0 0 24 24"
                  fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="••••••••••••"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (loginError) {
                      setLoginError(null);
                    }
                  }}
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="toggle-password"
                  onClick={() => setShowPassword(!showPassword)}
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                      stroke="currentColor" strokeWidth="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  ) : (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                      stroke="currentColor" strokeWidth="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="checkbox-group">
              <input
                id="remember"
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
              />
              <label htmlFor="remember">Recordar sesión en este equipo</label>
            </div>

            <button type="submit" className="login-button" disabled={loading}>
              {loading ? (
                <>
                  <span className="spinner" />
                  Iniciando sesión...
                </>
              ) : (
                <>
                  Iniciar Sesión
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                    stroke="currentColor" strokeWidth="2.5">
                    <path d="M5 12h14M12 5l7 7-7 7" />
                  </svg>
                </>
              )}
            </button>

          </form>

          <p className="login-footer">
            Acceso restringido a personal autorizado de LABERIT.<br />
            Las actividades son monitoreadas.
          </p>

        </div>
      </div>

    </div>
  );
};

export default LoginPage;