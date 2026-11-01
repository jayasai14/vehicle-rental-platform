import React from 'react';
import { Link } from 'react-router-dom';
import { Car, ShieldCheck, MapPin, CheckCircle, ArrowUpRight } from 'lucide-react';

export const Footer = () => {
  return (
    <footer className="border-t border-slate-800 bg-slate-950 pt-16 pb-12 text-slate-400 text-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10 pb-12 border-b border-slate-800/80">
          
          {/* Brand Col */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-emerald-400 flex items-center justify-center text-slate-950">
                <Car className="w-5 h-5 stroke-[2.5]" />
              </div>
              <span className="text-xl font-bold tracking-tight text-white">DriveElite</span>
            </div>
            <p className="text-slate-400 leading-relaxed pr-6">
              Vijayawada's trusted peer-to-partner vehicle rental marketplace. 
              Direct verified partner UPI collection, zero hidden platform markups, and guaranteed vehicle availability.
            </p>
            <div className="flex items-center gap-2 text-xs text-brand-400 font-medium">
              <ShieldCheck className="w-4 h-4" />
              <span>Partner-Direct Payments &bull; No Platform Escrow Risk</span>
            </div>
          </div>

          {/* Hub Locations in Vijayawada */}
          <div>
            <h4 className="text-white font-semibold text-sm mb-3 uppercase tracking-wider">Vijayawada Hubs</h4>
            <ul className="space-y-2 text-xs">
              <li className="flex items-center gap-2 hover:text-white transition-colors">
                <MapPin className="w-3.5 h-3.5 text-brand-400 shrink-0" />
                <span>Benz Circle Central Hub</span>
              </li>
              <li className="flex items-center gap-2 hover:text-white transition-colors">
                <MapPin className="w-3.5 h-3.5 text-brand-400 shrink-0" />
                <span>MG Road Commercial Point</span>
              </li>
              <li className="flex items-center gap-2 hover:text-white transition-colors">
                <MapPin className="w-3.5 h-3.5 text-brand-400 shrink-0" />
                <span>PNBS Bus Station Hub</span>
              </li>
              <li className="flex items-center gap-2 hover:text-white transition-colors">
                <MapPin className="w-3.5 h-3.5 text-brand-400 shrink-0" />
                <span>Gannavaram Airport Express</span>
              </li>
            </ul>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="text-white font-semibold text-sm mb-3 uppercase tracking-wider">Explore</h4>
            <ul className="space-y-2 text-xs">
              <li><Link to="/vehicles" className="hover:text-white transition-colors">All Rental Vehicles</Link></li>
              <li><Link to="/how-it-works" className="hover:text-white transition-colors">How Renting Works</Link></li>
              <li><Link to="/partners" className="hover:text-white transition-colors">Become a Rental Partner</Link></li>
              <li><Link to="/contributors" className="hover:text-white transition-colors">Monetize Your Idle Car</Link></li>
            </ul>
          </div>

          {/* Governance & Architecture */}
          <div>
            <h4 className="text-white font-semibold text-sm mb-3 uppercase tracking-wider">Architecture</h4>
            <ul className="space-y-2 text-xs">
              <li className="flex items-center gap-1.5 text-slate-300">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                <span>48 Verified Physical Tables</span>
              </li>
              <li className="flex items-center gap-1.5 text-slate-300">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                <span>Safeguard A: Late Payment Reapplication</span>
              </li>
              <li className="flex items-center gap-1.5 text-slate-300">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                <span>Safeguard B: Pessimistic Refund Locks</span>
              </li>
              <li className="flex items-center gap-1.5 text-slate-300">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                <span>ShedLock Distributed Tasks</span>
              </li>
            </ul>
          </div>

        </div>

        {/* Bottom bar */}
        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-4">
          <div>
            &copy; 2026 DriveElite Platform. All rights reserved. Built for Vijayawada, AP expansion.
          </div>
          <div className="flex items-center gap-6">
            <span>Direct Partner Settlement Model</span>
            <span>Zero-Trust Audit Logs</span>
            <span>Motor Vehicles Act Compliant</span>
          </div>
        </div>

      </div>
    </footer>
  );
};

export default Footer;
