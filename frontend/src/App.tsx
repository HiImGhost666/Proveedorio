import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from 'features/auth/context/AuthContext';
import LoginPage from 'features/auth/pages/LoginPage';
import InventoryDashboardPage from 'features/inventory/pages/InventoryDashboardPage';
import DashboardPage from '@/features/dashboard/pages/DashboardPage';
import AnalyticsPage from '@/features/dashboard/pages/AnalyticsPage';
import ProductDetailPage from 'features/inventory/pages/ProductDetailPage';
import NotFoundPage from 'pages/NotFoundPage';
import ProtectedRoute from 'router/ProtectedRoute';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SupplierListPage from 'features/suppliers/pages/SupplierListPage';
import SupplierDetailPage from 'features/suppliers/pages/SupplierDetailPage';
import SupplierFormPage from 'features/suppliers/pages/SupplierFormPage';
import SupplierDashboardPage from 'features/suppliers/pages/SupplierDashboardPage';
import UserListPage from 'features/users/pages/UserListPage';
import UserFormPage from 'features/users/pages/UserFormPage';
import PriceHistoryPage from './features/priceHistory/pages/PriceHistoryPage';
import CartPage from 'features/cart/pages/CartPage';
import { CartProvider } from 'features/cart/context/CartContext';
import ErrorBoundary from 'components/ui/ErrorBoundary';

const queryClient = new QueryClient();

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<InventoryDashboardPage />} />
        <Route path="/product/:id" element={<ProductDetailPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/suppliers/:id/dashboard" element={<SupplierDashboardPage />} />
        <Route path="/suppliers" element={<SupplierListPage />} />
        <Route path="/suppliers/new" element={<SupplierFormPage />} />
        <Route path="/suppliers/:id" element={<SupplierDetailPage />} />
        <Route path="/suppliers/:id/edit" element={<SupplierFormPage />} />
        <Route path="/users" element={<UserListPage />} />
        <Route path="/users/new" element={<UserFormPage />} />
        <Route path="/users/edit/:id" element={<UserFormPage />} />
        <Route path="/historial-precios/:productoId/:proveedorId" element={<PriceHistoryPage />} />
        <Route path="/carrito" element={<CartPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <AuthProvider>
          <CartProvider>
            <ErrorBoundary fallbackMessage="Ocurrió un error en la aplicación.">
              <AppRoutes />
            </ErrorBoundary>
          </CartProvider>
        </AuthProvider>
      </Router>
    </QueryClientProvider>
  );
}

export default App;
