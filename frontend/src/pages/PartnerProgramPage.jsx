import React from 'react';
import { Link } from 'react-router-dom';
import { Building2, ShieldCheck, DollarSign, Key, CheckCircle, ArrowRight } from 'lucide-react';

export const PartnerProgramPage = () => {
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-16">
      <div className="text-center space-y-4 max-w-2xl mx-auto">
        <span className="text-xs font-semibold uppercase tracking-wider text-blue-400">
          For Vehicle Rental Operators
        </span>
        <h1 className="text-3xl sm:text-5xl font-extrabold text-white">
          Become a Verified Rental Partner
        </h1>
        <p className="text-slate-400 text-sm sm:text-base leading-relaxed">
          Scale your vehicle rental fleet across Vijayawada. Direct UPI payments, digital pre-rental checklists, and contributor fleet expansion.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center font-bold">
            <DollarSign className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">100% Direct UPI Settlement</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Customers remit rental proceeds directly to your registered business UPI ID. Platform takes zero payment escrow cuts in MVP.
          </p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-brand-500/10 text-brand-400 flex items-center justify-center font-bold">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">Verified KYC &amp; Fleet Protection</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Every customer driving licence is verified against transport authority records. Audited condition notes at every handover.
          </p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-teal-500/10 text-teal-400 flex items-center justify-center font-bold">
            <Key className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">Contributor Fleet Onboarding</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Expand your rental capacity without vehicle capex. Manage vehicles contributed by local car owners with automated revenue splits.
          </p>
        </div>
      </div>

      <div className="glass-panel p-8 rounded-3xl border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-6">
        <div>
          <h3 className="text-xl font-bold text-white">Ready to operate as a partner in Vijayawada?</h3>
          <p className="text-xs text-slate-400 mt-1">Register your business details and get approved within 24 hours.</p>
        </div>
        <Link
          to="/register"
          className="px-6 py-3 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl text-xs transition-colors shrink-0"
        >
          Register as Rental Partner
        </Link>
      </div>
    </div>
  );
};

export default PartnerProgramPage;
