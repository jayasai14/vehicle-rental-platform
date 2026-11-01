import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';

// Pages
import HomePage from './pages/HomePage';
import VehiclesPage from './pages/VehiclesPage';
import VehicleDetailPage from './pages/VehicleDetailPage';
import CheckoutPage from './pages/CheckoutPage';
import HowItWorksPage from './pages/HowItWorksPage';
import PartnerProgramPage from './pages/PartnerProgramPage';
import ContributorFleetPage from './pages/ContributorFleetPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import CustomerDashboard from './pages/CustomerDashboard';
import PartnerDashboard from './pages/PartnerDashboard';
import ContributorDashboard from './pages/ContributorDashboard';
import AdminDashboard from './pages/AdminDashboard';

export const App = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex flex-col min-h-screen bg-slate-950 text-slate-100">
          <Navbar />
          <main className="flex-1">
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<HomePage />} />
              <Route path="/vehicles" element={<VehiclesPage />} />
              <Route path="/vehicles/:id" element={<VehicleDetailPage />} />
              <Route path="/how-it-works" element={<HowItWorksPage />} />
              <Route path="/partners" element={<PartnerProgramPage />} />
              <Route path="/contributors" element={<ContributorFleetPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Protected Checkout */}
              <Route 
                path="/checkout/:holdId" 
                element={
                  <ProtectedRoute>
                    <CheckoutPage />
                  </ProtectedRoute>
                } 
              />

              {/* Role Dashboards */}
              <Route 
                path="/dashboard/customer" 
                element={
                  <ProtectedRoute allowedRoles={['CUSTOMER', 'PARTNER', 'CONTRIBUTOR', 'ADMIN']}>
                    <CustomerDashboard />
                  </ProtectedRoute>
                } 
              />
              <Route 
                path="/dashboard/partner" 
                element={
                  <ProtectedRoute allowedRoles={['PARTNER', 'ADMIN']}>
                    <PartnerDashboard />
                  </ProtectedRoute>
                } 
              />
              <Route 
                path="/dashboard/contributor" 
                element={
                  <ProtectedRoute allowedRoles={['CONTRIBUTOR', 'ADMIN']}>
                    <ContributorDashboard />
                  </ProtectedRoute>
                } 
              />
              <Route 
                path="/dashboard/admin" 
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminDashboard />
                  </ProtectedRoute>
                } 
              />

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
          <Footer />
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
