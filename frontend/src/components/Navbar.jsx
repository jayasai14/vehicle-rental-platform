import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Car, 
  MapPin, 
  User, 
  LogOut, 
  Shield, 
  Briefcase, 
  Key, 
  ChevronDown, 
  Menu, 
  X,
  Sparkles,
  ArrowRight
} from 'lucide-react';

export const Navbar = () => {
  const { user, isAuthenticated, logout, switchDemoRole } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [demoMenuOpen, setDemoMenuOpen] = useState(false);
  const navigate = useNavigate();

  const handleRoleSwitch = async (role) => {
    setDemoMenuOpen(false);
    await switchDemoRole(role);
    if (role === 'CUSTOMER') navigate('/dashboard/customer');
    if (role === 'PARTNER') navigate('/dashboard/partner');
    if (role === 'CONTRIBUTOR') navigate('/dashboard/contributor');
    if (role === 'ADMIN') navigate('/dashboard/admin');
  };

  const getDashboardPath = () => {
    if (!user) return '/login';
    switch (user.role) {
      case 'PARTNER': return '/dashboard/partner';
      case 'CONTRIBUTOR': return '/dashboard/contributor';
      case 'ADMIN': return '/dashboard/admin';
      default: return '/dashboard/customer';
    }
  };

  return (
    <header className="sticky top-0 z-50 glass-panel border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20">
          
          {/* Logo & City Badge */}
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-3 group">
              <div className="w-11 h-11 rounded-xl bg-gradient-to-tr from-brand-600 to-emerald-400 flex items-center justify-center text-slate-950 shadow-lg shadow-brand-500/20 group-hover:scale-105 transition-transform">
                <Car className="w-6 h-6 stroke-[2.5]" />
              </div>
              <div className="flex flex-col">
                <span className="text-xl font-bold tracking-tight text-white flex items-center gap-1.5">
                  DriveElite <span className="text-brand-400 text-xs font-semibold px-2 py-0.5 rounded-full bg-brand-500/10 border border-brand-500/20">MVP</span>
                </span>
                <span className="text-xs text-slate-400 flex items-center gap-1 font-medium">
                  <MapPin className="w-3 h-3 text-brand-400" /> Vijayawada, AP
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center gap-1 text-sm font-medium text-slate-300 ml-4">
              <Link to="/vehicles" className="px-3.5 py-2 rounded-lg hover:text-white hover:bg-slate-800/60 transition-colors">
                Explore Fleet
              </Link>
              <Link to="/how-it-works" className="px-3.5 py-2 rounded-lg hover:text-white hover:bg-slate-800/60 transition-colors">
                How It Works
              </Link>
              <Link to="/partners" className="px-3.5 py-2 rounded-lg hover:text-white hover:bg-slate-800/60 transition-colors">
                For Rental Partners
              </Link>
              <Link to="/contributors" className="px-3.5 py-2 rounded-lg hover:text-white hover:bg-slate-800/60 transition-colors">
                Attach Your Car
              </Link>
            </nav>
          </div>

          {/* Right Action Bar */}
          <div className="hidden lg:flex items-center gap-3">
            
            {/* Quick Demo Role Switcher */}
            <div className="relative">
              <button 
                onClick={() => setDemoMenuOpen(!demoMenuOpen)}
                className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg bg-slate-900 border border-slate-700/80 text-amber-400 hover:border-amber-400/50 hover:bg-amber-950/20 transition-all shadow-sm"
                title="Switch demo persona instantly"
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Switch Persona</span>
                <ChevronDown className="w-3 h-3 opacity-70" />
              </button>

              {demoMenuOpen && (
                <div className="absolute right-0 mt-2 w-56 rounded-xl bg-slate-900 border border-slate-700 shadow-2xl p-1.5 z-50">
                  <div className="px-3 py-1.5 text-[11px] font-semibold text-slate-400 uppercase tracking-wider border-b border-slate-800">
                    Instant Demo Login
                  </div>
                  <button 
                    onClick={() => handleRoleSwitch('CUSTOMER')}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-xs text-left rounded-lg text-slate-200 hover:bg-slate-800 hover:text-emerald-400"
                  >
                    <User className="w-4 h-4 text-emerald-400" />
                    <div>
                      <div className="font-medium">Customer Persona</div>
                      <div className="text-[10px] text-slate-500">Book, pay UPI, manage holds</div>
                    </div>
                  </button>
                  <button 
                    onClick={() => handleRoleSwitch('PARTNER')}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-xs text-left rounded-lg text-slate-200 hover:bg-slate-800 hover:text-blue-400"
                  >
                    <Briefcase className="w-4 h-4 text-blue-400" />
                    <div>
                      <div className="font-medium">Rental Partner Persona</div>
                      <div className="text-[10px] text-slate-500">Fleet, verify UPI, inspections</div>
                    </div>
                  </button>
                  <button 
                    onClick={() => handleRoleSwitch('CONTRIBUTOR')}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-xs text-left rounded-lg text-slate-200 hover:bg-slate-800 hover:text-teal-400"
                  >
                    <Key className="w-4 h-4 text-teal-400" />
                    <div>
                      <div className="font-medium">Car Contributor Persona</div>
                      <div className="text-[10px] text-slate-500">Vehicles, agreements, earnings</div>
                    </div>
                  </button>
                  <button 
                    onClick={() => handleRoleSwitch('ADMIN')}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-xs text-left rounded-lg text-slate-200 hover:bg-slate-800 hover:text-purple-400"
                  >
                    <Shield className="w-4 h-4 text-purple-400" />
                    <div>
                      <div className="font-medium">Platform Admin Persona</div>
                      <div className="text-[10px] text-slate-500">Approvals, disputes, audit log</div>
                    </div>
                  </button>
                </div>
              )}
            </div>

            {/* User Account / Navigation */}
            {isAuthenticated ? (
              <div className="flex items-center gap-3 pl-2 border-l border-slate-800">
                <Link
                  to={getDashboardPath()}
                  className="flex items-center gap-2.5 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-brand-500/40 text-sm font-medium text-slate-200 hover:text-white transition-all"
                >
                  <div className="w-6 h-6 rounded-full bg-brand-500/20 text-brand-400 flex items-center justify-center text-xs font-bold">
                    {user?.fullName?.charAt(0) || 'U'}
                  </div>
                  <span>{user?.fullName || 'Dashboard'}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
                    {user?.role}
                  </span>
                </Link>

                <button
                  onClick={logout}
                  className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-slate-900 border border-transparent hover:border-slate-800 transition-colors"
                  title="Sign out"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2 pl-2">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-slate-950 font-semibold text-sm shadow-md shadow-brand-500/20 transition-all hover:scale-[1.02]"
                >
                  <span>Get Started</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            )}
          </div>

          {/* Mobile menu trigger */}
          <div className="flex items-center gap-2 lg:hidden">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>

        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden border-t border-slate-800 bg-slate-950 px-4 pt-4 pb-6 space-y-3">
          <Link 
            to="/vehicles" 
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-900"
          >
            Explore Fleet
          </Link>
          <Link 
            to="/how-it-works" 
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-900"
          >
            How It Works
          </Link>
          <Link 
            to="/partners" 
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-900"
          >
            For Rental Partners
          </Link>
          <Link 
            to="/contributors" 
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-900"
          >
            Attach Your Car
          </Link>

          <div className="pt-4 border-t border-slate-800">
            <div className="text-xs text-slate-500 uppercase font-semibold mb-2">Switch Demo Persona</div>
            <div className="grid grid-cols-2 gap-2">
              <button 
                onClick={() => { handleRoleSwitch('CUSTOMER'); setMobileMenuOpen(false); }}
                className="px-3 py-2 text-xs bg-slate-900 rounded-lg text-emerald-400 border border-slate-800 text-left"
              >
                Customer
              </button>
              <button 
                onClick={() => { handleRoleSwitch('PARTNER'); setMobileMenuOpen(false); }}
                className="px-3 py-2 text-xs bg-slate-900 rounded-lg text-blue-400 border border-slate-800 text-left"
              >
                Rental Partner
              </button>
              <button 
                onClick={() => { handleRoleSwitch('CONTRIBUTOR'); setMobileMenuOpen(false); }}
                className="px-3 py-2 text-xs bg-slate-900 rounded-lg text-teal-400 border border-slate-800 text-left"
              >
                Contributor
              </button>
              <button 
                onClick={() => { handleRoleSwitch('ADMIN'); setMobileMenuOpen(false); }}
                className="px-3 py-2 text-xs bg-slate-900 rounded-lg text-purple-400 border border-slate-800 text-left"
              >
                Admin
              </button>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-800 flex flex-col gap-2">
            {isAuthenticated ? (
              <>
                <Link
                  to={getDashboardPath()}
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center py-2.5 bg-brand-600 text-slate-950 font-bold rounded-xl"
                >
                  Go to {user?.role} Dashboard
                </Link>
                <button
                  onClick={() => { logout(); setMobileMenuOpen(false); }}
                  className="w-full text-center py-2 text-sm text-rose-400"
                >
                  Sign Out
                </button>
              </>
            ) : (
              <div className="flex gap-2">
                <Link
                  to="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex-1 text-center py-2.5 border border-slate-700 rounded-xl text-sm font-semibold"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex-1 text-center py-2.5 bg-brand-600 text-slate-950 font-bold rounded-xl text-sm"
                >
                  Register
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
};

export default Navbar;
