import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Car, 
  Lock, 
  Mail, 
  AlertCircle, 
  Sparkles, 
  ArrowRight,
  User,
  Briefcase,
  Key,
  Shield
} from 'lucide-react';

export const LoginPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please provide email and password');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const loggedUser = await login(email, password);
      if (from) {
        navigate(from, { replace: true });
      } else {
        if (loggedUser.role === 'PARTNER') navigate('/dashboard/partner');
        else if (loggedUser.role === 'CONTRIBUTOR') navigate('/dashboard/contributor');
        else if (loggedUser.role === 'ADMIN') navigate('/dashboard/admin');
        else navigate('/dashboard/customer');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid email or password. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const fillQuickDemo = (demoEmail, demoPass) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setError('');
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 glass-panel p-8 sm:p-10 rounded-3xl border border-slate-800 shadow-2xl relative">
        
        {/* Glow */}
        <div className="absolute -top-10 left-1/2 -translate-x-1/2 w-48 h-48 bg-brand-500/10 blur-[80px] pointer-events-none rounded-full" />

        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-brand-600 to-emerald-400 flex items-center justify-center text-slate-950 mx-auto shadow-lg shadow-brand-500/20">
            <Car className="w-6 h-6 stroke-[2.5]" />
          </div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight">Sign In to DriveElite</h2>
          <p className="text-xs text-slate-400">Vijayawada's Peer-to-Partner Vehicle Rental Marketplace</p>
        </div>

        {/* Demo Fast Fill Section */}
        <div className="p-3.5 bg-slate-950/80 rounded-2xl border border-slate-800 space-y-2.5">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-400">
            <Sparkles className="w-3.5 h-3.5" />
            <span>1-Click Test Persona Fill</span>
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <button
              type="button"
              onClick={() => fillQuickDemo('customer@example.com', 'Password@123')}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:border-emerald-500/50 hover:text-emerald-400 text-left flex items-center gap-2"
            >
              <User className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              <span className="truncate">Customer</span>
            </button>
            <button
              type="button"
              onClick={() => fillQuickDemo('partner@example.com', 'Password@123')}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:border-blue-500/50 hover:text-blue-400 text-left flex items-center gap-2"
            >
              <Briefcase className="w-3.5 h-3.5 text-blue-400 shrink-0" />
              <span className="truncate">Partner</span>
            </button>
            <button
              type="button"
              onClick={() => fillQuickDemo('contributor@example.com', 'Password@123')}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:border-teal-500/50 hover:text-teal-400 text-left flex items-center gap-2"
            >
              <Key className="w-3.5 h-3.5 text-teal-400 shrink-0" />
              <span className="truncate">Contributor</span>
            </button>
            <button
              type="button"
              onClick={() => fillQuickDemo('admin@example.com', 'Password@123')}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:border-purple-500/50 hover:text-purple-400 text-left flex items-center gap-2"
            >
              <Shield className="w-3.5 h-3.5 text-purple-400 shrink-0" />
              <span className="truncate">Admin</span>
            </button>
          </div>
        </div>

        {error && (
          <div className="p-3 bg-rose-950/40 border border-rose-500/30 rounded-xl text-xs text-rose-300 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Mail className="w-3.5 h-3.5 text-brand-400" />
              <span>Email Address</span>
            </label>
            <input
              type="email"
              required
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm focus:outline-none focus:border-brand-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Lock className="w-3.5 h-3.5 text-brand-400" />
              <span>Password</span>
            </label>
            <input
              type="password"
              required
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm focus:outline-none focus:border-brand-500"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-3 bg-brand-600 hover:bg-brand-500 disabled:bg-slate-800 disabled:text-slate-500 text-slate-950 font-bold rounded-xl shadow-lg shadow-brand-500/20 text-sm transition-all flex items-center justify-center gap-2 mt-2"
          >
            {submitting ? (
              <span>Authenticating...</span>
            ) : (
              <>
                <span>Sign In</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="text-center text-xs text-slate-400 pt-2 border-t border-slate-800/80">
          Don't have an account yet?{' '}
          <Link to="/register" className="text-brand-400 font-semibold hover:underline">
            Register here
          </Link>
        </div>

      </div>
    </div>
  );
};

export default LoginPage;
