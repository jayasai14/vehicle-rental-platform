import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  Clock, 
  ShieldCheck, 
  Building2, 
  QrCode, 
  CheckCircle2, 
  AlertTriangle, 
  ArrowRight, 
  RotateCcw,
  Sparkles,
  Car
} from 'lucide-react';
import { bookingApi, paymentApi } from '../api/endpoints';
import HoldCountdownTimer from '../components/HoldCountdownTimer';
import UPIPaymentModal from '../components/UPIPaymentModal';
import StatusBadge from '../components/StatusBadge';

export const CheckoutPage = () => {
  const { holdId } = useParams();
  const navigate = useNavigate();

  const [hold, setHold] = useState(null);
  const [demand, setDemand] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showPaymentModal, setShowPaymentModal] = useState(false);

  // Late payment reapplication state (Safeguard A)
  const [showReapplyBox, setShowReapplyBox] = useState(false);
  const [oldSubmissionId, setOldSubmissionId] = useState('');
  const [reapplying, setReapplying] = useState(false);
  const [reapplySuccess, setReapplySuccess] = useState('');
  const [reapplyError, setReapplyError] = useState('');

  const loadCheckoutData = async () => {
    try {
      setLoading(true);
      const holdRes = await bookingApi.getHold(holdId);
      setHold(holdRes.data?.data);

      const demandRes = await paymentApi.getDemandByHold(holdId);
      setDemand(demandRes.data?.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load hold reservation.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCheckoutData();
  }, [holdId]);

  // Handle Safeguard A late payment recovery
  const handleReapplyPayment = async (e) => {
    e.preventDefault();
    if (!oldSubmissionId.trim()) return;

    setReapplying(true);
    setReapplyError('');
    setReapplySuccess('');

    try {
      const payload = {
        originalSubmissionId: parseInt(oldSubmissionId.trim()),
        newHoldId: parseInt(holdId)
      };

      await paymentApi.reapplyPayment(payload);
      setReapplySuccess('Success! Late payment reallocated to this hold.');
      // Reload demand
      await loadCheckoutData();
    } catch (err) {
      setReapplyError(err.response?.data?.message || 'Could not reapply payment. Verify submission ID is verified and unapplied.');
    } finally {
      setReapplying(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error || !hold) {
    return (
      <div className="max-w-md mx-auto py-20 text-center space-y-4">
        <AlertTriangle className="w-12 h-12 text-rose-400 mx-auto" />
        <h2 className="text-xl font-bold text-white">Hold Expired or Not Found</h2>
        <p className="text-sm text-slate-400">{error || 'This vehicle reservation hold is no longer active.'}</p>
        <Link to="/vehicles" className="inline-block px-5 py-2.5 bg-brand-600 text-slate-950 font-bold rounded-xl text-xs">
          Return to Fleet Catalogue
        </Link>
      </div>
    );
  }

  const isSatisfied = demand?.status === 'SATISFIED';
  const isHoldExpired = hold?.status === 'EXPIRED';

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-brand-400">
            Checkout &bull; Direct Partner Settlement
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white mt-1">
            Complete Booking Reservation
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <StatusBadge status={hold.status} />
          {demand && <StatusBadge status={demand.status} />}
        </div>
      </div>

      {/* Countdown Timer */}
      <HoldCountdownTimer 
        expiresAt={hold.expiresAt} 
        onExpired={() => {
          setHold(prev => ({ ...prev, status: 'EXPIRED' }));
        }} 
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        
        {/* Left Col: Vehicle & Demand Details */}
        <div className="md:col-span-2 space-y-6">
          
          {/* Reservation Card */}
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center shrink-0">
                <Car className="w-8 h-8 text-brand-400" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">{hold.vehicleModel}</h3>
                <p className="text-xs text-slate-400 font-mono">Reg: {hold.vehiclePlateNumber || 'AP 16 XX 0001'}</p>
                <p className="text-xs text-slate-400 mt-1">Pickup Hub: <strong>{hold.pickupLocationName || 'Vijayawada Central'}</strong></p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-3 border-t border-slate-800/80 text-xs">
              <div>
                <span className="text-slate-500">Rental Start:</span>
                <div className="font-semibold text-white mt-0.5">
                  {new Date(hold.startTime).toLocaleString()}
                </div>
              </div>
              <div>
                <span className="text-slate-500">Rental End:</span>
                <div className="font-semibold text-white mt-0.5">
                  {new Date(hold.endTime).toLocaleString()}
                </div>
              </div>
            </div>
          </div>

          {/* Payment Demand Breakdown */}
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider text-slate-400">
              Payment Demand #{demand?.demandReference || 'DEM-001'}
            </h3>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between items-center text-slate-300">
                <span>Rental Proceeds Demanded:</span>
                <span className="font-mono font-bold text-white">
                  ₹{Number(demand?.amountDemanded || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>

              <div className="flex justify-between items-center text-slate-400 text-xs">
                <span>Verified Satisfied Amount:</span>
                <span className="font-mono text-emerald-400 font-semibold">
                  ₹{Number(demand?.amountSatisfied || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>

              <div className="flex justify-between items-center text-slate-400 text-xs">
                <span>Direct Beneficiary:</span>
                <span className="text-white font-medium">
                  {demand?.partnerBusinessName || 'Sri Krishna Rentals'}
                </span>
              </div>

              <div className="pt-3 border-t border-slate-800 flex justify-between items-center text-base">
                <span className="font-bold text-white">Total Pending Balance:</span>
                <span className="text-2xl font-extrabold text-emerald-400 font-mono">
                  ₹{Math.max(0, (demand?.amountDemanded || 0) - (demand?.amountSatisfied || 0)).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>
            </div>

            {/* Submissions List if any */}
            {demand?.submissions && demand.submissions.length > 0 && (
              <div className="pt-4 border-t border-slate-800 space-y-2">
                <h4 className="text-xs font-semibold text-slate-300">Submitted Payments for this Demand:</h4>
                {demand.submissions.map((s) => (
                  <div key={s.id} className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between text-xs">
                    <div>
                      <div className="font-mono font-bold text-white">UTR: {s.transactionReference}</div>
                      <div className="text-[11px] text-slate-400">₹{s.amountSubmitted} via {s.paymentMethod}</div>
                    </div>
                    <StatusBadge status={s.submissionStatus} />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Safeguard A: Late Payment Reapplication Box */}
          <div className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
            <button
              type="button"
              onClick={() => setShowReapplyBox(!showReapplyBox)}
              className="w-full flex items-center justify-between text-xs font-semibold text-amber-400 hover:text-amber-300"
            >
              <div className="flex items-center gap-2">
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Safeguard A: Paid for an expired hold earlier? Reapply funds here</span>
              </div>
              <span>{showReapplyBox ? '▲ Hide' : '▼ Expand'}</span>
            </button>

            {showReapplyBox && (
              <form onSubmit={handleReapplyPayment} className="pt-3 border-t border-slate-800 space-y-3">
                <p className="text-[11px] text-slate-400 leading-relaxed">
                  If you previously sent money for a hold that expired before partner verification, 
                  our platform safeguards allow you to reapply that exact verified amount to this new reservation hold!
                </p>

                {reapplySuccess && (
                  <div className="p-2.5 bg-emerald-950/40 border border-emerald-500/30 rounded-xl text-xs text-emerald-300">
                    {reapplySuccess}
                  </div>
                )}
                {reapplyError && (
                  <div className="p-2.5 bg-rose-950/40 border border-rose-500/30 rounded-xl text-xs text-rose-300">
                    {reapplyError}
                  </div>
                )}

                <div className="flex gap-2">
                  <input
                    type="number"
                    placeholder="Enter original Submission ID (e.g. 1)"
                    value={oldSubmissionId}
                    onChange={(e) => setOldSubmissionId(e.target.value)}
                    className="flex-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs font-mono focus:outline-none focus:border-brand-500"
                  />
                  <button
                    type="submit"
                    disabled={reapplying}
                    className="px-4 py-2 bg-amber-500 hover:bg-amber-400 disabled:bg-slate-800 text-slate-950 font-bold text-xs rounded-xl transition-colors"
                  >
                    {reapplying ? 'Reapplying...' : 'Reapply Funds'}
                  </button>
                </div>
              </form>
            )}
          </div>

        </div>

        {/* Right Col: Action & Status Card */}
        <div className="space-y-6">
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-5">
            
            {isSatisfied ? (
              <div className="text-center space-y-4 py-4">
                <CheckCircle2 className="w-14 h-14 text-emerald-400 mx-auto animate-bounce" />
                <h3 className="text-lg font-bold text-white">Payment Demands Satisfied!</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  The partner has verified your payment credit. Your vehicle reservation is now confirmed!
                </p>
                <Link
                  to="/dashboard/customer"
                  className="w-full py-3 bg-brand-600 hover:bg-brand-500 text-slate-950 font-bold rounded-xl text-xs inline-flex items-center justify-center gap-2"
                >
                  <span>Go to My Bookings</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            ) : isHoldExpired ? (
              <div className="text-center space-y-4 py-4">
                <AlertTriangle className="w-12 h-12 text-rose-400 mx-auto" />
                <h3 className="text-base font-bold text-white">Hold Period Expired</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  The 15-minute exclusivity window elapsed. If you have already transferred money via UPI, copy your submission reference and use Safeguard A reapplication on a new hold.
                </p>
                <Link
                  to="/vehicles"
                  className="w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-white font-semibold rounded-xl text-xs inline-block"
                >
                  Choose Another Time
                </Link>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="border-b border-slate-800 pb-3">
                  <span className="text-xs uppercase text-slate-400 font-semibold">Payment Step</span>
                  <h4 className="text-base font-bold text-white mt-0.5">Partner Direct Pay</h4>
                </div>

                <div className="p-3 bg-slate-950 rounded-xl border border-slate-800/80 space-y-2 text-xs">
                  <div className="flex items-center gap-2 text-slate-300">
                    <Building2 className="w-4 h-4 text-brand-400" />
                    <span>Payee: <strong>{demand?.partnerBusinessName}</strong></span>
                  </div>
                  <div className="text-[11px] text-slate-400">
                    UPI VPA: <span className="font-mono text-brand-300 font-bold">{demand?.partnerUpiId || 'srirental@okaxis'}</span>
                  </div>
                </div>

                <button
                  onClick={() => setShowPaymentModal(true)}
                  className="w-full py-3 bg-brand-600 hover:bg-brand-500 text-slate-950 font-bold rounded-xl shadow-lg shadow-brand-500/20 text-sm transition-all flex items-center justify-center gap-2"
                >
                  <QrCode className="w-4 h-4" />
                  <span>Pay via Partner UPI Now</span>
                </button>

                <p className="text-[11px] text-slate-400 text-center">
                  Instant scan &amp; pay via Google Pay, PhonePe, Paytm, or BHIM.
                </p>
              </div>
            )}

            <div className="pt-4 border-t border-slate-800 flex items-center gap-2 text-[11px] text-slate-400">
              <ShieldCheck className="w-4 h-4 text-brand-400 shrink-0" />
              <span>Compliant with Motor Vehicles Act &amp; RBI UPI peer settlement guidelines.</span>
            </div>

          </div>
        </div>

      </div>

      {/* UPI Payment Modal */}
      {showPaymentModal && demand && (
        <UPIPaymentModal
          demand={demand}
          hold={hold}
          onSuccess={() => {
            setShowPaymentModal(false);
            loadCheckoutData();
          }}
          onClose={() => setShowPaymentModal(false)}
        />
      )}

    </div>
  );
};

export default CheckoutPage;
