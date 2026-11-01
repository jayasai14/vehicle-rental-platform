import React from 'react';

const statusConfig = {
  // Booking Statuses
  AWAITING_PAYMENT: { bg: 'bg-amber-500/10 text-amber-400 border-amber-500/20', label: 'Awaiting Payment' },
  CONFIRMED: { bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20', label: 'Confirmed' },
  IN_PROGRESS: { bg: 'bg-blue-500/10 text-blue-400 border-blue-500/20', label: 'In Progress (Active)' },
  COMPLETED: { bg: 'bg-slate-500/10 text-slate-300 border-slate-500/20', label: 'Completed' },
  CANCELLED: { bg: 'bg-rose-500/10 text-rose-400 border-rose-500/20', label: 'Cancelled' },

  // Demand / Hold Statuses
  ACTIVE: { bg: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20', label: 'Active Hold' },
  EXPIRED: { bg: 'bg-slate-700/30 text-slate-400 border-slate-700/50', label: 'Expired' },
  OPEN: { bg: 'bg-amber-500/10 text-amber-400 border-amber-500/20', label: 'Open Demand' },
  SATISFIED: { bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20', label: 'Satisfied' },

  // Submission Statuses
  SUBMITTED: { bg: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20', label: 'Pending Partner Verification' },
  VERIFIED: { bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20', label: 'Verified & Accepted' },
  REJECTED: { bg: 'bg-rose-500/10 text-rose-400 border-rose-500/20', label: 'Payment Rejected' },

  // Custody Statuses
  PARTNER_CUSTODY: { bg: 'bg-purple-500/10 text-purple-400 border-purple-500/20', label: 'Partner Custody' },
  CUSTOMER_CUSTODY: { bg: 'bg-blue-500/10 text-blue-400 border-blue-500/20', label: 'Customer Custody' },
  CONTRIBUTOR_CUSTODY: { bg: 'bg-teal-500/10 text-teal-400 border-teal-500/20', label: 'Contributor Custody' },

  // Refund Statuses
  PENDING_PARTNER_DISBURSEMENT: { bg: 'bg-amber-500/10 text-amber-400 border-amber-500/20', label: 'Pending Partner Disbursement' },
  DISBURSED_BY_PARTNER: { bg: 'bg-blue-500/10 text-blue-400 border-blue-500/20', label: 'Disbursed (Awaiting Ack)' },
  CUSTOMER_ACKNOWLEDGED: { bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20', label: 'Customer Confirmed' },
  DISPUTED_BY_CUSTOMER: { bg: 'bg-rose-500/10 text-rose-400 border-rose-500/20', label: 'Disputed by Customer' },
  DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED: { bg: 'bg-slate-600/20 text-slate-400 border-slate-600/30', label: 'Window Elapsed' },
  ADMIN_SETTLED_CUSTOMER_REFUNDED: { bg: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20', label: 'Admin Settled (Refunded)' },
  ADMIN_SETTLED_PARTNER_UPHELD: { bg: 'bg-slate-500/10 text-slate-400 border-slate-500/20', label: 'Admin Upheld Partner' },

  // Contributor Payable Statuses
  ACCRUED: { bg: 'bg-amber-500/10 text-amber-400 border-amber-500/20', label: 'Accrued' },
  SETTLED_BY_PARTNER: { bg: 'bg-blue-500/10 text-blue-400 border-blue-500/20', label: 'Settled by Partner' },
  CONFIRMED_BY_CONTRIBUTOR: { bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20', label: 'Contributor Confirmed' },
  DISPUTED_BY_CONTRIBUTOR: { bg: 'bg-rose-500/10 text-rose-400 border-rose-500/20', label: 'Settlement Disputed' },
};

export const StatusBadge = ({ status, className = '' }) => {
  if (!status) return null;
  const config = statusConfig[status] || {
    bg: 'bg-slate-800 text-slate-300 border-slate-700',
    label: status.replace(/_/g, ' ')
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${config.bg} ${className}`}>
      <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5 animate-pulse" />
      {config.label}
    </span>
  );
};
export default StatusBadge;
