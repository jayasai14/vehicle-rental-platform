import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/endpoints';

export const normalizeUser = (raw) => {
  if (!raw) return null;
  const u = raw.user || raw;

  const rawRoles = u.roles || raw.roles || [];
  const roles = (Array.isArray(rawRoles) ? rawRoles : Array.from(rawRoles)).map(r => {
    const str = typeof r === 'string' ? r : (r?.name || '');
    return str.replace(/^ROLE_/, '').replace(/^RENTAL_/, '').replace(/^VEHICLE_/, '');
  });

  let primaryRole = 'CUSTOMER';
  if (roles.includes('ADMIN')) primaryRole = 'ADMIN';
  else if (roles.includes('PARTNER') || roles.includes('RENTAL_PARTNER')) primaryRole = 'PARTNER';
  else if (roles.includes('CONTRIBUTOR') || roles.includes('VEHICLE_CONTRIBUTOR')) primaryRole = 'CONTRIBUTOR';
  else if (roles.length > 0) primaryRole = roles[0];

  return {
    id: u.id || raw.userId || u.userId,
    fullName: u.fullName || raw.fullName || '',
    email: u.email || raw.email || '',
    phoneNumber: u.phoneNumber || raw.phoneNumber || '',
    roles,
    role: primaryRole,
    customerProfile: u.customerProfile || null,
    drivingLicence: u.drivingLicence || null,
    emailVerified: !!u.emailVerified,
    phoneVerified: !!u.phoneVerified,
    active: u.active !== false,
  };
};

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user');
      return stored ? normalizeUser(JSON.parse(stored)) : null;
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
            const normalized = normalizeUser(res.data.data);
            setUser(normalized);
            localStorage.setItem('user', JSON.stringify(normalized));
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
    const data = res.data.data;
    const authUser = normalizeUser(data.user || data);
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(authUser));
    setToken(data.accessToken);
    setUser(authUser);
    return authUser;
  };

  const register = async (data) => {
    const res = await authApi.register(data);
    const respData = res.data.data;
    const authUser = normalizeUser(respData.user || respData);
    localStorage.setItem('accessToken', respData.accessToken);
    localStorage.setItem('refreshToken', respData.refreshToken);
    localStorage.setItem('user', JSON.stringify(authUser));
    setToken(respData.accessToken);
    setUser(authUser);
    return authUser;
  };

  const logout = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        await authApi.logout(refreshToken);
      }
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

  // Quick switcher for demo and testing convenience (Development only)
  const switchDemoRole = async (targetRole) => {
    if (!import.meta.env.DEV) {
      throw new Error('Demo persona switcher is restricted to local development');
    }
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
    isCustomer: user?.role === 'CUSTOMER' || user?.roles?.includes('CUSTOMER'),
    isPartner: user?.role === 'PARTNER' || user?.roles?.includes('PARTNER') || user?.roles?.includes('RENTAL_PARTNER'),
    isContributor: user?.role === 'CONTRIBUTOR' || user?.roles?.includes('CONTRIBUTOR') || user?.roles?.includes('VEHICLE_CONTRIBUTOR'),
    isAdmin: user?.role === 'ADMIN' || user?.roles?.includes('ADMIN'),
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

export default AuthContext;
