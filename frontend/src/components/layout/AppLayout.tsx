import { ReactNode } from 'react';
import { useAuth } from 'features/auth/context/AuthContext';
import { CartProvider } from 'features/cart/context/CartContext';
import { useCart } from 'features/cart/context/useCart';
import logo from '@/assets/ProveedoresLogo_Dark.png';
import { Toaster, toast } from 'react-hot-toast';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { syncAllProducts } from 'features/inventory/services/productService';
import ErrorNotificationBell from 'features/errors/components/ErrorNotificationBell';
import { BarChart3, LineChart, LogOut, Package, RefreshCw, ShoppingCart, Truck, Users } from 'lucide-react';
import './AppLayout.css';

const AppLayoutContent = ({ children }: { children: ReactNode }) => {
  const { username, logout, isAdmin } = useAuth();
  const { itemCount } = useCart();

  const syncMutation = useMutation({
    mutationFn: syncAllProducts,
    onSuccess: () => toast.success('Sincronización iniciada en segundo plano.'),
    onError: () => toast.error('No se pudo iniciar la sincronización.'),
  });

  return (
    <div className="app-layout">
      <Toaster position="top-right" />
      <header className="app-header">
        <div className="header-container">
          <Link to="/" className="header-logo-link">
            <img src={logo} alt="Inventario" className="header-logo" />
          </Link>
          <nav>
            <Link to="/" className="nav-link">
              <Package size={16} aria-hidden="true" />
              Productos
            </Link>
            <Link to="/dashboard" className="nav-link">
              <BarChart3 size={16} aria-hidden="true" />
              Dashboard
            </Link>
            <Link to="/analytics" className="nav-link">
              <LineChart size={16} aria-hidden="true" />
              Analytics
            </Link>
            <Link to="/suppliers" className="nav-link">
              <Truck size={16} aria-hidden="true" />
              Proveedores
            </Link>
            {isAdmin && (
              <Link to="/users" className="nav-link">
                <Users size={16} aria-hidden="true" />
                Usuarios
              </Link>
            )}
            {isAdmin && (
              <button
                onClick={() => syncMutation.mutate()}
                disabled={syncMutation.isPending}
                className="btn btn-primary sync-button"
                style={{ marginLeft: '1rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}
              >
                <RefreshCw size={16} className={syncMutation.isPending ? 'spin-icon' : ''} aria-hidden="true" />
                {syncMutation.isPending ? 'Iniciando...' : 'Sincronizar'}
              </button>
            )}
          </nav>
          <div className="user-info">
            <ErrorNotificationBell />
            <Link
              to="/carrito"
              className="cart-link"
              title="Ver carrito"
            >
              <ShoppingCart size={22} aria-hidden="true" />
              {itemCount > 0 && (
                <span className="cart-badge">
                  {itemCount}
                </span>
              )}
            </Link>
            <span className="user-greeting">Hola, {username}</span>
            <button
              onClick={logout}
              className="logout-button"
            >
              <LogOut size={16} aria-hidden="true" />
              Cerrar Sesión
            </button>
          </div>
        </div>
      </header>
      <main className="app-main">
        {children}
      </main>
    </div>
  );
};

const AppLayout = ({ children }: { children: ReactNode }) => {
  return (
    <CartProvider>
      <AppLayoutContent>{children}</AppLayoutContent>
    </CartProvider>
  );
};

export default AppLayout;
