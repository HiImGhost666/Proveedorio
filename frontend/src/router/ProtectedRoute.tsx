import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from 'features/auth/context/AuthContext';
import AppLayout from 'components/layout/AppLayout';
import { ErrorProvider } from 'features/errors/context/ErrorContext';

const ProtectedRoute = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <ErrorProvider>
      <AppLayout>
        <Outlet />
      </AppLayout>
    </ErrorProvider>
  );
};

export default ProtectedRoute;

