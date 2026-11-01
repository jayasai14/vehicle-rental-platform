import React, { useState, useEffect } from 'react';
import { 
  Building2, 
  Car, 
  CheckCircle2, 
  XCircle, 
  QrCode, 
  AlertCircle, 
  Key, 
  RotateCcw, 
  DollarSign, 
  ClipboardCheck, 
  PlusCircle, 
  ArrowRight,
  TrendingUp,
  MapPin,
  Clock
} from 'lucide-react';
import { partnerApi, operationsApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';

export const PartnerDashboard = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('payments'); // 'payments' | 'fleet' | 'handover' | 'settlements'
  const [fleet, setFleet] = useState([]);
  const [pendingPayments, setPendingPayments] = useState([]);
  const [payables, setPayables] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  // Rejection modal
  const [rejectingSubmission, setRejectingSubmission] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');

  // Settle payable modal
  const [settlingPayable, setSettlingPayable] = useState(null);
  const [settleUtr, setSettleUtr] = useState('');

  // Handover modal
  const [handoverBookingId, setHandoverBookingId] = useState('');
  const [handoverOdo, setHandoverOdo] = useState('15200');
  const [handoverFuel, setHandoverFuel] = useState('100');
  const [handoverNotes, setHandoverNotes] = useState('Clean vehicle, no scratches observed.');

  // Return modal
  const [returnBookingId, setReturnBookingId] = useState('');
  const [returnOdo, setReturnOdo] = useState('15540');
  const [returnFuel, setReturnFuel] = useState('95');
  const [returnDamageFee, setReturnDamageFee] = useState('0');
  const [returnNotes, setReturnNotes] = useState('Normal usage, verified vehicle intact.');

  const loadData = async () => {
    try {
      setLoading(true);
      const [fleetRes, payRes, payablesRes] = await Promise.all([
        partnerApi.getFleet(),
        partnerApi.getPendingPayments(),
        partnerApi.getContributorPayables()
      ]);
      setFleet(fleetRes.data?.data || []);
      setPendingPayments(payRes.data?.data || []);
      setPayables(payablesRes.data?.data || []);
    } catch (err) {
      console.error(err);
      setError('Could not load partner dashboard data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleVerifyPayment = async (subId) => {
    try {
      await partnerApi.verifyPayment(subId);
      setMessage(`Payment submission #${subId} verified! Demand satisfied & booking confirmed.`);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Verification failed.');
    }
  };

  const handleRejectPayment = async (e) => {
    e.preventDefault();
    if (!rejectingSubmission || !rejectionReason.trim()) return;
    try {
      await partnerApi.rejectPayment(rejectingSubmission.id, rejectionReason.trim());
      setMessage(`Payment #${rejectingSubmission.id} rejected.`);
      setRejectingSubmission(null);
      setRejectionReason('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Rejection failed.');
    }
  };

  const handleSettlePayable = async (e) => {
    e.preventDefault();
    if (!settlingPayable || !settleUtr.trim()) return;
    try {
      await partnerApi.recordPayableSettlement(settlingPayable.id, {
        paymentMethod: 'UPI',
        partnerTransactionReference: settleUtr.trim()
      });
      setMessage(`Payable #${settlingPayable.id} marked as SETTLED. Contributor notified.`);
      setSettlingPayable(null);
      setSettleUtr('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Settlement failed.');
    }
  };

  const handleRecordHandover = async (e) => {
    e.preventDefault();
    if (!handoverBookingId) return;
    try {
      await partnerApi.recordHandover(parseInt(handoverBookingId), {
        odometerReading: parseInt(handoverOdo),
        fuelPercent: parseInt(handoverFuel),
        conditionNotes: handoverNotes
      });
      setMessage(`Handover confirmed! Custody moved to CUSTOMER, booking is IN_PROGRESS.`);
      setHandoverBookingId('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Handover failed.');
    }
  };

  const handleRecordReturn = async (e) => {
    e.preventDefault();
    if (!returnBookingId) return;
    try {
      await partnerApi.recordReturn(parseInt(returnBookingId), {
        odometerReading: parseInt(returnOdo),
        fuelPercent: parseInt(returnFuel),
        conditionNotes: returnNotes,
        damageDeductionProposed: parseFloat(returnDamageFee)
      });
      setMessage(`Return completed! Vehicle in PARTNER_CUSTODY, booking COMPLETED, contributor cut accrued.`);
      setReturnBookingId('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Return recording failed.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-blue-400">
            Partner Control Center
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white mt-1">
            {user?.fullName || 'Partner Operations'} &bull; Fleet &amp; Settlements
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Direct customer payment verification, vehicle inspections, and contributor payouts.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <span className="px-3 py-1.5 rounded-xl bg-blue-500/10 border border-blue-500/30 text-blue-400 text-xs font-bold font-mono">
            Direct UPI: {user?.partnerUpiId || 'srirental@okaxis'}
          </span>
        </div>
      </div>

      {/* Messages */}
      {message && (
        <div className="p-3.5 bg-emerald-950/40 border border-emerald-500/30 rounded-xl text-xs text-emerald-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{message}</span>
          </div>
          <button onClick={() => setMessage('')} className="text-slate-400 hover:text-white">✕</button>
        </div>
      )}
      {error && (
        <div className="p-3.5 bg-rose-950/40 border border-rose-500/30 rounded-xl text-xs text-rose-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError('')} className="text-slate-400 hover:text-white">✕</button>
        </div>
      )}

      {/* Nav Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-slate-800 pb-2 text-xs font-semibold">
        <button
          onClick={() => setActiveTab('payments')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'payments'
              ? 'bg-blue-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Payment Verification Queue ({pendingPayments.length})
        </button>
        <button
          onClick={() => setActiveTab('fleet')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'fleet'
              ? 'bg-blue-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          My Fleet ({fleet.length})
        </button>
        <button
          onClick={() => setActiveTab('handover')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'handover'
              ? 'bg-blue-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Handover &amp; Return Handlers
        </button>
        <button
          onClick={() => setActiveTab('settlements')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'settlements'
              ? 'bg-blue-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Contributor Payables ({payables.length})
        </button>
      </div>

      {loading ? (
        <div className="min-h-[40vh] flex items-center justify-center">
          <div className="w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : activeTab === 'payments' ? (
        /* Payment Verification Queue */
        pendingPayments.length === 0 ? (
          <div className="glass-card p-12 rounded-2xl text-center space-y-4">
            <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
            <h3 className="text-lg font-bold text-white">Payment queue is clear</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              All submitted customer UPI payments for your fleet have been verified. New submissions will appear here in real-time.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="p-4 bg-blue-950/20 border border-blue-500/30 rounded-2xl flex items-center justify-between text-xs">
              <span className="text-blue-300">
                You are the <strong>Direct Merchant of Record</strong>. Verify each transaction against your UPI app before approving.
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {pendingPayments.map((sub) => (
                <div key={sub.id} className="glass-card p-6 rounded-2xl border border-slate-800 space-y-4">
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="text-[10px] font-mono text-slate-500 uppercase">
                        Ref: {sub.submissionReference}
                      </span>
                      <h4 className="text-base font-bold text-white mt-0.5">
                        Demand #{sub.demandId}
                      </h4>
                      <p className="text-xs text-slate-400">Customer: <strong>{sub.customerName}</strong></p>
                    </div>
                    <div className="text-right">
                      <div className="text-2xl font-extrabold text-emerald-400 font-mono">
                        ₹{Number(sub.amountSubmitted).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </div>
                      <span className="text-[10px] text-slate-400 uppercase">{sub.paymentMethod}</span>
                    </div>
                  </div>

                  <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 space-y-1.5 text-xs">
                    <div className="flex justify-between">
                      <span className="text-slate-500">Bank UTR / Transaction Ref:</span>
                      <span className="font-mono font-bold text-white tracking-wider">{sub.transactionReference}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Submitted At:</span>
                      <span className="text-slate-300">{new Date(sub.transactionTimestamp).toLocaleString()}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 pt-2">
                    <button
                      onClick={() => handleVerifyPayment(sub.id)}
                      className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-xl text-xs transition-colors flex items-center justify-center gap-1.5"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Verify &amp; Confirm Booking</span>
                    </button>
                    <button
                      onClick={() => setRejectingSubmission(sub)}
                      className="px-4 py-2.5 border border-rose-500/40 text-rose-300 hover:bg-rose-950/40 font-semibold rounded-xl text-xs transition-colors"
                    >
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )
      ) : activeTab === 'fleet' ? (
        /* Fleet Management */
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {fleet.map((v) => (
              <div key={v.id} className="glass-card rounded-2xl overflow-hidden p-5 space-y-4">
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="text-base font-bold text-white">{v.make} {v.model}</h4>
                    <p className="text-xs text-slate-400 font-mono">AP {v.plateNumber}</p>
                  </div>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
                    {v.status}
                  </span>
                </div>

                <div className="text-xs text-slate-400 space-y-1">
                  <div>Hub: <span className="text-white">{v.partnerHubName}</span></div>
                  <div>Daily Rate: <span className="text-emerald-400 font-mono font-bold">₹{v.dailyRate}</span></div>
                  <div>Custody: <span className="text-brand-300 font-mono">{v.custodyState}</span></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : activeTab === 'handover' ? (
        /* Handover & Return Inspector */
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          
          {/* Handover Form */}
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Key className="w-4 h-4 text-brand-400" />
              <span>Pre-Rental Vehicle Handover</span>
            </h3>
            <p className="text-xs text-slate-400">
              Deliver keys to customer. Moves vehicle custody to CUSTOMER_CUSTODY and booking to IN_PROGRESS.
            </p>

            <form onSubmit={handleRecordHandover} className="space-y-3 text-xs">
              <div>
                <label className="text-slate-300 font-semibold">Booking ID *</label>
                <input
                  type="number"
                  required
                  placeholder="e.g. 1"
                  value={handoverBookingId}
                  onChange={(e) => setHandoverBookingId(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-semibold">Odometer Reading (KM) *</label>
                  <input
                    type="number"
                    required
                    value={handoverOdo}
                    onChange={(e) => setHandoverOdo(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono"
                  />
                </div>
                <div>
                  <label className="text-slate-300 font-semibold">Fuel Level (%) *</label>
                  <input
                    type="number"
                    required
                    value={handoverFuel}
                    onChange={(e) => setHandoverFuel(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="text-slate-300 font-semibold">Inspection Notes</label>
                <textarea
                  rows={2}
                  value={handoverNotes}
                  onChange={(e) => setHandoverNotes(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl transition-colors"
              >
                Complete Pre-Rental Handover
              </button>
            </form>
          </div>

          {/* Return Form */}
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <ClipboardCheck className="w-4 h-4 text-emerald-400" />
              <span>Post-Rental Vehicle Return</span>
            </h3>
            <p className="text-xs text-slate-400">
              Receive vehicle at hub. Returns custody to PARTNER_CUSTODY, completes booking, and generates contributor payables.
            </p>

            <form onSubmit={handleRecordReturn} className="space-y-3 text-xs">
              <div>
                <label className="text-slate-300 font-semibold">Booking ID *</label>
                <input
                  type="number"
                  required
                  placeholder="e.g. 1"
                  value={returnBookingId}
                  onChange={(e) => setReturnBookingId(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-semibold">Return Odometer (KM) *</label>
                  <input
                    type="number"
                    required
                    value={returnOdo}
                    onChange={(e) => setReturnOdo(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono"
                  />
                </div>
                <div>
                  <label className="text-slate-300 font-semibold">Return Fuel (%) *</label>
                  <input
                    type="number"
                    required
                    value={returnFuel}
                    onChange={(e) => setReturnFuel(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="text-slate-300 font-semibold">Damage Deduction Proposed (₹)</label>
                <input
                  type="number"
                  value={returnDamageFee}
                  onChange={(e) => setReturnDamageFee(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono"
                />
              </div>

              <div>
                <label className="text-slate-300 font-semibold">Return Inspection Notes</label>
                <textarea
                  rows={2}
                  value={returnNotes}
                  onChange={(e) => setReturnNotes(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-xl transition-colors"
              >
                Complete Return &amp; Accrue Payout
              </button>
            </form>
          </div>

        </div>
      ) : (
        /* Contributor Payables */
        payables.length === 0 ? (
          <div className="glass-card p-12 rounded-2xl text-center space-y-4">
            <DollarSign className="w-12 h-12 text-slate-600 mx-auto" />
            <h3 className="text-lg font-bold text-white">No contributor payables</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Completed bookings on contributor-owned vehicles will automatically calculate agreed cut obligations here.
            </p>
          </div>
        ) : (
          <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-900 border-b border-slate-800 text-[11px] text-slate-400 uppercase font-semibold">
                  <tr>
                    <th className="py-3 px-4">Payable ID</th>
                    <th className="py-3 px-4">Booking Code</th>
                    <th className="py-3 px-4">Agreement ID</th>
                    <th className="py-3 px-4">Agreed Cut</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Partner Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {payables.map((p) => (
                    <tr key={p.id} className="hover:bg-slate-900/40">
                      <td className="py-3 px-4 font-mono font-bold text-white">#{p.id}</td>
                      <td className="py-3 px-4 font-semibold text-white">{p.bookingCode}</td>
                      <td className="py-3 px-4 font-mono">AGR-{p.agreementId}</td>
                      <td className="py-3 px-4 font-mono font-bold text-emerald-400 text-sm">
                        ₹{Number(p.agreedCutAmount).toFixed(2)}
                      </td>
                      <td className="py-3 px-4">
                        <StatusBadge status={p.payableStatus} />
                      </td>
                      <td className="py-3 px-4 text-right">
                        {p.payableStatus === 'ACCRUED' && (
                          <button
                            onClick={() => setSettlingPayable(p)}
                            className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-lg text-xs transition-colors"
                          >
                            Record UPI Settlement
                          </button>
                        )}
                        {p.payableStatus === 'SETTLED_BY_PARTNER' && (
                          <span className="text-slate-400 text-[11px]">Awaiting Contributor Ack</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )
      )}

      {/* Rejection Modal */}
      {rejectingSubmission && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-white">Reject Payment #{rejectingSubmission.id}</h3>
            <form onSubmit={handleRejectPayment} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs text-slate-300 font-semibold">Reason for Rejection *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. UTR 4291849201 not found on bank statement"
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setRejectingSubmission(null)}
                  className="px-4 py-2 text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-xl"
                >
                  Confirm Rejection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Settle Payable Modal */}
      {settlingPayable && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-white">Settle Payable #{settlingPayable.id}</h3>
            <p className="text-xs text-slate-300">
              Amount to transfer: <span className="font-mono font-bold text-emerald-400">₹{settlingPayable.agreedCutAmount}</span>
            </p>
            <form onSubmit={handleSettlePayable} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs text-slate-300 font-semibold">Partner UPI UTR Reference *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 592819402819"
                  value={settleUtr}
                  onChange={(e) => setSettleUtr(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs font-mono"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSettlingPayable(null)}
                  className="px-4 py-2 text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs rounded-xl"
                >
                  Confirm Settlement
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

export default PartnerDashboard;
