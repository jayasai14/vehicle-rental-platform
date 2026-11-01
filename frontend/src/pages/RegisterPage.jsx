import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Car, 
  Lock, 
  Mail, 
  User, 
  Phone, 
  Briefcase, 
  Building2, 
  AlertCircle, 
  ArrowRight,
  ShieldCheck 
} from 'lucide-react';

export const RegisterPage = () => {
  const [role, setRole] = useState('CUSTOMER');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  
  // Partner specific fields
  const [businessName, setBusinessName] = useState('');
  const [gstin, setGstin] = useState('');
  const [upiId, setUpiId] = useState('');

  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');

    try {
      const payload = {
        fullName,
        email,
        phoneNumber,
        password,
        role,
        ...(role === 'PARTNER' && {
          businessName,
          gstin,
          partnerUpiId: upiId
        })
      };

      const user = await register(payload);
      if (user.role === 'PARTNER') navigate('/dashboard/partner');
      else if (user.role === 'CONTRIBUTOR') navigate('/dashboard/contributor');
      else navigate('/dashboard/customer');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-lg w-full space-y-6 glass-panel p-8 sm:p-10 rounded-3xl border border-slate-800 shadow-2xl relative">
        
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-brand-600 to-emerald-400 flex items-center justify-center text-slate-950 mx-auto shadow-lg shadow-brand-500/20">
            <Car className="w-6 h-6 stroke-[2.5]" />
          </div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight">Create an Account</h2>
          <p className="text-xs text-slate-400">Join DriveElite vehicle rental platform in Vijayawada</p>
        </div>

        {/* Role Selector */}
        <div className="grid grid-cols-3 gap-2 p-1.5 bg-slate-950 rounded-2xl border border-slate-800 text-xs font-semibold">
          <button
            type="button"
            onClick={() => setRole('CUSTOMER')}
            className={`py-2 px-3 rounded-xl transition-all ${
              role === 'CUSTOMER'
                ? 'bg-brand-600 text-slate-950 font-bold shadow-md'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Customer
          </button>
          <button
            type="button"
            onClick={() => setRole('PARTNER')}
            className={`py-2 px-3 rounded-xl transition-all ${
              role === 'PARTNER'
                ? 'bg-blue-600 text-white font-bold shadow-md'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Rental Partner
          </button>
          <button
            type="button"
            onClick={() => setRole('CONTRIBUTOR')}
            className={`py-2 px-3 rounded-xl transition-all ${
              role === 'CONTRIBUTOR'
                ? 'bg-teal-600 text-white font-bold shadow-md'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Contributor
          </button>
        </div>

        {error && (
          <div className="p-3 bg-rose-950/40 border border-rose-500/30 rounded-xl text-xs text-rose-300 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">Full Legal Name *</label>
              <input
                type="text"
                required
                placeholder="Ravi Kumar"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">Mobile Number *</label>
              <input
                type="tel"
                required
                placeholder="+91 9876543210"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300">Email Address *</label>
            <input
              type="email"
              required
              placeholder="ravi@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300">Password *</label>
            <input
              type="password"
              required
              placeholder="Minimum 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
            />
          </div>

          {/* Partner Specific Inputs */}
          {role === 'PARTNER' && (
            <div className="p-4 rounded-2xl bg-blue-950/20 border border-blue-500/30 space-y-3">
              <span className="text-xs font-bold text-blue-300 uppercase">Partner Business Info</span>
              <div className="space-y-1.5">
                <label className="text-xs text-slate-300">Registered Business Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Amaravati Self-Drive Rentals"
                  value={businessName}
                  onChange={(e) => setBusinessName(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <label className="text-xs text-slate-300">GSTIN (Optional)</label>
                  <input
                    type="text"
                    placeholder="37AAAAA0000A1Z5"
                    value={gstin}
                    onChange={(e) => setGstin(e.target.value)}
                    className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs text-slate-300">Business UPI ID *</label>
                  <input
                    type="text"
                    required
                    placeholder="partner@okhdfcbank"
                    value={upiId}
                    onChange={(e) => setUpiId(e.target.value)}
                    className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-3 bg-brand-600 hover:bg-brand-500 disabled:bg-slate-800 disabled:text-slate-500 text-slate-950 font-bold rounded-xl shadow-lg shadow-brand-500/20 text-sm transition-all flex items-center justify-center gap-2 mt-2"
          >
            {submitting ? (
              <span>Registering...</span>
            ) : (
              <>
                <span>Complete Registration</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="text-center text-xs text-slate-400 pt-2 border-t border-slate-800/80">
          Already registered?{' '}
          <Link to="/login" className="text-brand-400 font-semibold hover:underline">
            Sign In here
          </Link>
        </div>

      </div>
    </div>
  );
};

export default RegisterPage;
