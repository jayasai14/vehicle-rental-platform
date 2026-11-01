import React, { useState, useEffect } from 'react';
import { Clock, AlertTriangle } from 'lucide-react';

export const HoldCountdownTimer = ({ expiresAt, onExpired }) => {
  const calculateRemainingSeconds = () => {
    if (!expiresAt) return 0;
    const diff = new Date(expiresAt).getTime() - new Date().getTime();
    return Math.max(0, Math.floor(diff / 1000));
  };

  const [remaining, setRemaining] = useState(calculateRemainingSeconds);

  useEffect(() => {
    setRemaining(calculateRemainingSeconds());
    const interval = setInterval(() => {
      const rem = calculateRemainingSeconds();
      setRemaining(rem);
      if (rem <= 0) {
        clearInterval(interval);
        if (onExpired) onExpired();
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [expiresAt]);

  const minutes = Math.floor(remaining / 60);
  const seconds = remaining % 60;
  const isUrgent = remaining < 180; // less than 3 minutes
  const isExpired = remaining <= 0;

  // Percentage based on 15 minutes (900 seconds)
  const percent = Math.min(100, Math.max(0, (remaining / 900) * 100));

  if (isExpired) {
    return (
      <div className="flex items-center gap-2 p-3 bg-rose-950/40 border border-rose-500/30 rounded-xl text-rose-300 text-sm">
        <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
        <span><strong>Hold Expired:</strong> Vehicle reservation time window elapsed. If payment was sent, you may reapply it below.</span>
      </div>
    );
  }

  return (
    <div className={`p-4 rounded-xl border transition-all ${
      isUrgent 
        ? 'bg-rose-950/30 border-rose-500/40 text-rose-200' 
        : 'bg-emerald-950/30 border-emerald-500/30 text-emerald-200'
    }`}>
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2 font-medium text-sm">
          <Clock className={`w-4 h-4 ${isUrgent ? 'text-rose-400 animate-spin' : 'text-emerald-400'}`} />
          <span>Vehicle Hold Active</span>
        </div>
        <div className="font-mono font-bold text-lg tracking-wider">
          {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
        </div>
      </div>
      <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
        <div 
          className={`h-full transition-all duration-1000 rounded-full ${
            isUrgent ? 'bg-rose-500' : 'bg-emerald-500'
          }`}
          style={{ width: `${percent}%` }}
        />
      </div>
      <p className="text-xs text-slate-400 mt-2">
        Reserved exclusively for you. Submit payment reference before the timer ends to guarantee booking.
      </p>
    </div>
  );
};

export default HoldCountdownTimer;
