import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const ProtectedRoute = ({ children, allowedRoles = [] }) => {
  const { user, isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(user?.role)) {
    // If logged in but unauthorized role, redirect to appropriate home or dashboard
    if (user.role === 'PARTNER') return <Navigate to="/dashboard/partner" replace />;
    if (user.role === 'CONTRIBUTOR') return <Navigate to="/dashboard/contributor" replace />;
    if (user.role === 'ADMIN') return <Navigate to="/dashboard/admin" replace />;
    return <Navigate to="/dashboard/customer" replace />;
  }

  return children;
};

export default ProtectedRoute;
