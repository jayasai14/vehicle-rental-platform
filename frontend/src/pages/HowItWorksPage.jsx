import React from 'react';
import { Link } from 'react-router-dom';
import { 
  Search, 
  Clock, 
  QrCode, 
  Key, 
  RotateCcw, 
  ShieldCheck, 
  ArrowRight,
  CheckCircle2 
} from 'lucide-react';

export const HowItWorksPage = () => {
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-16">
      <div className="text-center space-y-4 max-w-2xl mx-auto">
        <span className="text-xs font-semibold uppercase tracking-wider text-brand-400">
          Transparent Self-Drive Rentals
        </span>
        <h1 className="text-3xl sm:text-5xl font-extrabold text-white">
          How DriveElite Works in Vijayawada
        </h1>
        <p className="text-slate-400 text-sm sm:text-base leading-relaxed">
          From vehicle discovery to key handover and return inspection — designed for safety, transparency, and speed.
        </p>
      </div>

      {/* Step by Step Timeline */}
      <div className="space-y-8">
        {[
          {
            step: '01',
            icon: Search,
            title: 'Explore & Choose Your Vehicle',
            desc: 'Browse certified SUVs, sedans, and EVs available across Benz Circle, MG Road, PNBS, or Airport hubs in Vijayawada. Review daily rates and mileage limits.',
            highlight: 'Zero Platform Convenience Fee'
          },
          {
            step: '02',
            icon: Clock,
            title: 'Place a 15-Minute Exclusivity Hold',
            desc: 'When you choose your pickup and return dates, our engine creates a strict 15-minute hold on the vehicle calendar, ensuring nobody else books it.',
            highlight: 'Guaranteed Availability'
          },
          {
            step: '03',
            icon: QrCode,
            title: 'Direct Partner UPI Payment & UTR Entry',
            desc: 'Pay the demanded rental proceeds directly to the verified partner’s business UPI (Google Pay, PhonePe, Paytm). Enter your 12-digit UTR reference.',
            highlight: 'Direct Settlement • No Platform Escrow Delay'
          },
          {
            step: '04',
            icon: Key,
            title: 'Vehicle Handover & Return Inspection',
            desc: 'Collect keys at the partner hub. The partner records pre-rental odometer & fuel. On return, condition is inspected and security deposit is released.',
            highlight: 'Audited Condition Handover'
          }
        ].map((item, idx) => (
          <div key={idx} className="glass-panel p-6 sm:p-8 rounded-3xl border border-slate-800 flex flex-col sm:flex-row items-start gap-6">
            <div className="w-14 h-14 rounded-2xl bg-brand-500/10 border border-brand-500/30 text-brand-400 font-extrabold text-xl flex items-center justify-center shrink-0">
              {item.step}
            </div>
            <div className="space-y-2 flex-1">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-lg font-bold text-white">{item.title}</h3>
                <span className="text-[11px] font-semibold text-emerald-400 px-2.5 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/20">
                  {item.highlight}
                </span>
              </div>
              <p className="text-slate-400 text-xs sm:text-sm leading-relaxed">{item.desc}</p>
            </div>
          </div>
        ))}
      </div>

      {/* CTA Box */}
      <div className="glass-card p-8 rounded-3xl border border-brand-500/30 text-center space-y-4">
        <h3 className="text-xl font-bold text-white">Ready for your journey in Vijayawada?</h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto">
          Reserve your vehicle with instant 15-minute lock and drive without hassle.
        </p>
        <Link
          to="/vehicles"
          className="inline-flex items-center gap-2 px-6 py-3 bg-brand-600 hover:bg-brand-500 text-slate-950 font-bold rounded-xl text-sm shadow-lg shadow-brand-500/20 transition-all"
        >
          <span>Find Available Cars</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    </div>
  );
};

export default HowItWorksPage;
