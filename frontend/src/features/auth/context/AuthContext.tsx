// AuthContext.tsx
import { createContext, useState, useContext, ReactNode } from 'react';
import { AuthContextType, AuthResponse, DecodedToken } from 'src/types';
import { jwtDecode } from 'jwt-decode'; // named import




const AuthContext = createContext<AuthContextType | undefined>(undefined);

const getInitialAuthState = () => {
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');

  if (!token) {
    return { token: null, username: null, isAdmin: false, roles: [] };
  }

  try {
    const decodedToken = jwtDecode(token) as DecodedToken;
    const isExpired = decodedToken.exp * 1000 < Date.now();

    if (isExpired) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      return { token: null, username: null, isAdmin: false, roles: [] };
    }

    const roles = decodedToken.roles || [];
    const isAdmin = roles.includes('ROLE_ADMIN');
    return { token, username, isAdmin, roles };
  } catch (error) {
    console.error('Error decoding initial token:', error);
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    return { token: null, username: null, isAdmin: false, roles: [] };
  }
};


export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [authState, setAuthState] = useState(getInitialAuthState);

  const login = (authResponse: AuthResponse) => {
    const { token, username } = authResponse;
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);

    try {
      const decodedToken = jwtDecode(token) as DecodedToken;
      const roles = decodedToken.roles || [];
      const isAdmin = roles.includes('ROLE_ADMIN');
      setAuthState({ token, username, isAdmin, roles });
    } catch (error) {
      console.error('Error decoding token on login:', error);
      // Still log in, but without admin rights if token is weird
      setAuthState({ token, username, isAdmin: false, roles: [] });
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setAuthState({ token: null, username: null, isAdmin: false, roles: [] });
  };

  const isAuthenticated = !!authState.token;

  return (
    <AuthContext.Provider value={{ ...authState, login, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
};

// Hook para consumir el contexto
// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthContextProvider');
  }
  return context;
};