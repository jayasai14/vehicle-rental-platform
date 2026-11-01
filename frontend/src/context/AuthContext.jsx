import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/endpoints';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('accessToken') || null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const handleLogout = () => {
      setUser(null);
      setToken(null);
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    };

    window.addEventListener('auth-logout', handleLogout);
    return () => window.removeEventListener('auth-logout', handleLogout);
  }, []);

  useEffect(() => {
    const checkAuth = async () => {
      const savedToken = localStorage.getItem('accessToken');
      if (savedToken) {
        try {
          const res = await authApi.getProfile();
          if (res.data?.data) {
            setUser(res.data.data);
            localStorage.setItem('user', JSON.stringify(res.data.data));
          }
        } catch (err) {
          console.warn('Session verification error:', err);
        }
      }
      setLoading(false);
    };

    checkAuth();
  }, [token]);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });
    const { accessToken, refreshToken, user: authUser } = res.data.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(authUser));
    setToken(accessToken);
    setUser(authUser);
    return authUser;
  };

  const register = async (data) => {
    const res = await authApi.register(data);
    const { accessToken, refreshToken, user: authUser } = res.data.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(authUser));
    setToken(accessToken);
    setUser(authUser);
    return authUser;
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors on logout
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      setToken(null);
      setUser(null);
    }
  };

  // Quick switcher for demo and testing convenience
  const switchDemoRole = async (targetRole) => {
    const demoAccounts = {
      CUSTOMER: { email: 'customer@example.com', pass: 'Password@123' },
      PARTNER: { email: 'partner@example.com', pass: 'Password@123' },
      CONTRIBUTOR: { email: 'contributor@example.com', pass: 'Password@123' },
      ADMIN: { email: 'admin@example.com', pass: 'Password@123' }
    };
    const target = demoAccounts[targetRole];
    if (target) {
      return await login(target.email, target.pass);
    }
  };

  const value = {
    user,
    token,
    loading,
    login,
    register,
    logout,
    switchDemoRole,
    isAuthenticated: !!token && !!user,
    role: user?.role || null,
    isCustomer: user?.role === 'CUSTOMER',
    isPartner: user?.role === 'PARTNER',
    isContributor: user?.role === 'CONTRIBUTOR',
    isAdmin: user?.role === 'ADMIN',
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
