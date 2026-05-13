import { Link } from 'react-router-dom';
import './NotFoundPage.css';

const NotFoundPage = () => {
  return (
    <div className="not-found-container">
      <h1 className="not-found-title">404</h1>
      <p className="not-found-text">Página no encontrada</p>
      <Link to="/" className="not-found-link">
        Volver al inicio
      </Link>
    </div>
  );
};

export default NotFoundPage;
