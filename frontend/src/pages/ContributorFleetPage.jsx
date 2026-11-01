import React from 'react';
import { Link } from 'react-router-dom';
import { Key, Car, DollarSign, FileCheck, ArrowRight, ShieldCheck } from 'lucide-react';

export const ContributorFleetPage = () => {
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-16">
      <div className="text-center space-y-4 max-w-2xl mx-auto">
        <span className="text-xs font-semibold uppercase tracking-wider text-teal-400">
          Car Monetization in Vijayawada
        </span>
        <h1 className="text-3xl sm:text-5xl font-extrabold text-white">
          Turn Your Idle Car into Monthly Income
        </h1>
        <p className="text-slate-400 text-sm sm:text-base leading-relaxed">
          Attach your vehicle to a certified rental partner. Sign a legally binding agreement, receive transparent revenue shares, and withdraw anytime.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-teal-500/10 text-teal-400 flex items-center justify-center font-bold">
            <DollarSign className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">70% Monthly Revenue Share</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Earn 70% of gross realized rental revenue for completed trips. Partner settles proceeds directly into your bank via UPI.
          </p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-brand-500/10 text-brand-400 flex items-center justify-center font-bold">
            <FileCheck className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">Digitally Executed Agreements</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Clear contractual obligations defining maintenance, operational custody, insurance liabilities, and return conditions.
          </p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center font-bold">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">Full Custody State Machine</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Every movement between Contributor, Partner, and Customer is audited. Request vehicle withdrawal whenever needed.
          </p>
        </div>
      </div>

      <div className="glass-panel p-8 rounded-3xl border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-6">
        <div>
          <h3 className="text-xl font-bold text-white">Ready to attach your car?</h3>
          <p className="text-xs text-slate-400 mt-1">Submit your vehicle registration and connect with a certified hub.</p>
        </div>
        <Link
          to="/register"
          className="px-6 py-3 bg-teal-600 hover:bg-teal-500 text-slate-950 font-bold rounded-xl text-xs transition-colors shrink-0"
        >
          Register as Contributor
        </Link>
      </div>
    </div>
  );
};

export default ContributorFleetPage;
