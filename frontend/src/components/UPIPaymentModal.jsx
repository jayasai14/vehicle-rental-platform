import React, { useState } from 'react';
import { 
  QrCode, 
  Copy, 
  Check, 
  Upload, 
  AlertCircle, 
  CheckCircle2, 
  ArrowRight, 
  ShieldCheck,
  Building2,
  Phone
} from 'lucide-react';
import { paymentApi } from '../api/endpoints';
import HoldCountdownTimer from './HoldCountdownTimer';

export const UPIPaymentModal = ({ 
  demand, 
  hold, 
  onSuccess, 
  onClose 
}) => {
  const [copied, setCopied] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [transactionRef, setTransactionRef] = useState('');
  const [notes, setNotes] = useState('');
  const [receiptFile, setReceiptFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const upiId = demand?.partnerUpiId || 'srirental@okaxis';
  const partnerName = demand?.partnerBusinessName || 'Sri Krishna Rentals';
  const demandedAmount = demand?.amountDemanded || '0.00';

  const handleCopyUpi = () => {
    navigator.clipboard.writeText(upiId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!transactionRef.trim()) {
      setError('Please enter the 12-digit UPI UTR / Bank Reference Number');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      let receiptKey = null;
      if (receiptFile) {
        // Mock receipt upload or use reference
        receiptKey = `receipts/${Date.now()}_${receiptFile.name}`;
      }

      const payload = {
        demandId: demand.id,
        paymentMethod: paymentMethod,
        transactionReference: transactionRef.trim(),
        amountSubmitted: parseFloat(demandedAmount),
        receiptObjectKey: receiptKey,
        customerNotes: notes.trim()
      };

      await paymentApi.submitPayment(payload);
      setSuccess(true);
      setTimeout(() => {
        if (onSuccess) onSuccess();
      }, 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit payment reference. Please check your connection.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm overflow-y-auto">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6">
        
        {/* Header */}
        <div className="flex items-start justify-between border-b border-slate-800 pb-4">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-brand-400">
              Direct Partner Collection
            </span>
            <h3 className="text-xl font-bold text-white mt-1">Complete Rental Payment</h3>
          </div>
          {onClose && (
            <button 
              onClick={onClose}
              className="text-slate-400 hover:text-white text-sm font-semibold p-1"
            >
              ✕
            </button>
          )}
        </div>

        {/* Hold Timer */}
        {hold && (
          <HoldCountdownTimer expiresAt={hold.expiresAt} />
        )}

        {/* Success Banner */}
        {success ? (
          <div className="p-6 bg-emerald-950/40 border border-emerald-500/40 rounded-xl text-center space-y-3">
            <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto animate-bounce" />
            <h4 className="text-lg font-bold text-white">Payment Submission Recorded!</h4>
            <p className="text-sm text-emerald-200">
              Your reference <span className="font-mono font-bold">{transactionRef}</span> has been dispatched to {partnerName}.
              The partner will verify the credit and confirm your booking.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-5">
            
            {/* Amount & Partner Info Card */}
            <div className="p-4 bg-slate-950/80 rounded-xl border border-slate-800 space-y-3">
              <div className="flex justify-between items-center text-sm">
                <span className="text-slate-400">Total Demanded Amount:</span>
                <span className="text-2xl font-bold text-emerald-400 font-mono">
                  ₹{Number(demandedAmount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div className="text-xs text-slate-400 flex items-center justify-between pt-2 border-t border-slate-800/80">
                <div className="flex items-center gap-1.5 text-slate-300">
                  <Building2 className="w-3.5 h-3.5 text-brand-400" />
                  <span>Beneficiary: <strong>{partnerName}</strong></span>
                </div>
                <span className="text-emerald-400 font-medium">Direct Settlement</span>
              </div>
            </div>

            {/* UPI QR & ID Box */}
            <div className="p-4 bg-slate-950 border border-brand-500/30 rounded-xl flex flex-col sm:flex-row items-center gap-5">
              {/* QR Representation */}
              <div className="w-32 h-32 bg-white p-2 rounded-xl flex flex-col items-center justify-center shrink-0 shadow-md">
                {/* Visual SVG QR representation */}
                <svg className="w-28 h-28" viewBox="0 0 100 100" fill="black">
                  <path d="M0,0 h30 v30 h-30 z M5,5 v20 h20 v-20 z M10,10 h10 v10 h-10 z" />
                  <path d="M70,0 h30 v30 h-30 z M75,5 v20 h20 v-20 z M80,10 h10 v10 h-10 z" />
                  <path d="M0,70 h30 v30 h-30 z M5,75 v20 h20 v-20 z M10,80 h10 v10 h-10 z" />
                  <rect x="35" y="10" width="8" height="8" />
                  <rect x="50" y="10" width="12" height="8" />
                  <rect x="38" y="25" width="20" height="8" />
                  <rect x="15" y="40" width="8" height="20" />
                  <rect x="35" y="40" width="30" height="20" />
                  <rect x="75" y="40" width="15" height="15" />
                  <rect x="40" y="70" width="10" height="25" />
                  <rect x="60" y="65" width="25" height="10" />
                  <rect x="75" y="80" width="20" height="15" />
                </svg>
                <span className="text-[9px] font-bold text-slate-800 mt-1 uppercase">Scan &amp; Pay UPI</span>
              </div>

              {/* UPI Details */}
              <div className="flex-1 space-y-2 text-center sm:text-left">
                <span className="text-xs text-slate-400 font-medium">Partner UPI VPA / ID:</span>
                <div className="flex items-center justify-between bg-slate-900 border border-slate-700/80 px-3 py-2 rounded-lg">
                  <span className="font-mono text-xs sm:text-sm text-brand-300 font-semibold truncate mr-2">
                    {upiId}
                  </span>
                  <button
                    type="button"
                    onClick={handleCopyUpi}
                    className="p-1 text-slate-400 hover:text-white transition-colors"
                    title="Copy UPI ID"
                  >
                    {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
                <p className="text-[11px] text-slate-400">
                  Open Google Pay, PhonePe, Paytm, or BHIM, transfer the exact amount, then paste the 12-digit UTR below.
                </p>
              </div>
            </div>

            {/* Error Message */}
            {error && (
              <div className="p-3 bg-rose-950/40 border border-rose-500/30 rounded-xl text-xs text-rose-300 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Transaction Reference Input */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 flex items-center justify-between">
                <span>UPI Reference Number (UTR / Transaction ID) *</span>
                <span className="text-[11px] text-slate-400 font-normal">12 digits</span>
              </label>
              <input
                type="text"
                required
                maxLength={30}
                placeholder="e.g. 429381749201"
                value={transactionRef}
                onChange={(e) => setTransactionRef(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-sm placeholder:text-slate-600 focus:outline-none focus:border-brand-500"
              />
            </div>

            {/* Optional Customer Notes */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Payment Remark / Remitter Name (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g. Sent from HDFC account of Rajesh"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full px-4 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-brand-500"
              />
            </div>

            {/* Submit Action */}
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 bg-brand-600 hover:bg-brand-500 disabled:bg-slate-800 disabled:text-slate-500 text-slate-950 font-bold rounded-xl shadow-lg shadow-brand-500/20 transition-all flex items-center justify-center gap-2"
            >
              {submitting ? (
                <span>Recording Submission...</span>
              ) : (
                <>
                  <span>Submit Payment Reference to Partner</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>

            <div className="flex items-center justify-center gap-2 text-[11px] text-slate-400">
              <ShieldCheck className="w-3.5 h-3.5 text-brand-400" />
              <span>Partner-direct transaction. Protected by platform audit trail.</span>
            </div>

          </form>
        )}

      </div>
    </div>
  );
};

export default UPIPaymentModal;
