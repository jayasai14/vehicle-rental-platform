import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Car, 
  Clock, 
  CheckCircle2, 
  AlertCircle, 
  AlertTriangle, 
  RotateCcw, 
  FileText, 
  ShieldCheck, 
  DollarSign, 
  X,
  MessageSquarePlus,
  ArrowRight
} from 'lucide-react';
import { bookingApi, paymentApi, operationsApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';

export const CustomerDashboard = () => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('bookings'); // 'bookings' | 'refunds' | 'disputes'

  // Cancel booking modal
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelSubmitting, setCancelSubmitting] = useState(false);

  // Dispute refund modal
  const [disputingRefund, setDisputingRefund] = useState(null);
  const [disputeReason, setDisputeReason] = useState('');
  const [disputeSubmitting, setDisputeSubmitting] = useState(false);

  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      const [bookingsRes, refundsRes] = await Promise.all([
        bookingApi.getMyBookings(),
        paymentApi.getMyRefunds()
      ]);
      setBookings(bookingsRes.data?.data || []);
      setRefunds(refundsRes.data?.data || []);
    } catch (err) {
      console.error(err);
      setError('Could not load dashboard data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCancelBooking = async (e) => {
    e.preventDefault();
    if (!cancellingBooking) return;
    setCancelSubmitting(true);
    setError('');
    try {
      await bookingApi.cancelBooking(cancellingBooking.id, cancelReason);
      setMessage(`Booking ${cancellingBooking.bookingCode} successfully cancelled.`);
      setCancellingBooking(null);
      setCancelReason('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to cancel booking.');
    } finally {
      setCancelSubmitting(false);
    }
  };

  const handleAcknowledgeRefund = async (refundId) => {
    try {
      await paymentApi.acknowledgeRefund(refundId);
      setMessage('Refund receipt acknowledged successfully! Status updated.');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to acknowledge refund.');
    }
  };

  const handleDisputeRefund = async (e) => {
    e.preventDefault();
    if (!disputingRefund || !disputeReason.trim()) return;
    setDisputeSubmitting(true);
    try {
      await paymentApi.disputeRefund(disputingRefund.id, disputeReason.trim());
      setMessage('Dispute successfully raised. Ticket escalated to Admin Arbitration.');
      setDisputingRefund(null);
      setDisputeReason('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit refund dispute.');
    } finally {
      setDisputeSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      
      {/* Welcome Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-brand-400">
            Customer Dashboard
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white mt-1">
            Welcome back, {user?.fullName || 'Traveler'}
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Manage your Vijayawada self-drive vehicle bookings, track refunds, and verify deposits.
          </p>
        </div>

        <Link
          to="/vehicles"
          className="px-4 py-2.5 bg-brand-600 hover:bg-brand-500 text-slate-950 font-bold text-xs rounded-xl shadow-md transition-all flex items-center gap-1.5 self-start sm:self-auto"
        >
          <Car className="w-4 h-4" />
          <span>Book Another Vehicle</span>
        </Link>
      </div>

      {/* Notifications / Alerts */}
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

      {/* Navigation Tabs */}
      <div className="flex gap-2 border-b border-slate-800 pb-2 text-xs font-semibold">
        <button
          onClick={() => setActiveTab('bookings')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'bookings'
              ? 'bg-brand-500/10 text-brand-400 border border-brand-500/30'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          My Bookings ({bookings.length})
        </button>
        <button
          onClick={() => setActiveTab('refunds')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'refunds'
              ? 'bg-brand-500/10 text-brand-400 border border-brand-500/30'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Rental Fee Refunds ({refunds.length})
        </button>
      </div>

      {loading ? (
        <div className="min-h-[40vh] flex items-center justify-center">
          <div className="w-10 h-10 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : activeTab === 'bookings' ? (
        /* Bookings List */
        bookings.length === 0 ? (
          <div className="glass-card p-12 rounded-2xl text-center space-y-4">
            <Car className="w-12 h-12 text-slate-600 mx-auto" />
            <h3 className="text-lg font-bold text-white">No active bookings yet</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              You haven't reserved any vehicles yet. Explore our fleet in Vijayawada and book directly with local partners.
            </p>
            <Link to="/vehicles" className="inline-block px-4 py-2 bg-brand-600 text-slate-950 font-bold text-xs rounded-xl">
              Find Cars Now
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {bookings.map((booking) => (
              <div key={booking.id} className="glass-card p-6 rounded-2xl border border-slate-800 space-y-4">
                <div className="flex items-start justify-between">
                  <div>
                    <span className="text-[10px] font-mono text-slate-500 uppercase">
                      Code: {booking.bookingCode}
                    </span>
                    <h3 className="text-base font-bold text-white mt-0.5">
                      {booking.vehicleModel}
                    </h3>
                    <p className="text-xs text-slate-400 font-mono">Reg: {booking.vehiclePlateNumber}</p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <StatusBadge status={booking.status} />
                    <span className="text-[10px] text-slate-400 font-mono">
                      Custody: {booking.custodyState}
                    </span>
                  </div>
                </div>

                <div className="p-3 bg-slate-950/80 rounded-xl border border-slate-800/80 grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <span className="text-slate-500">Pickup:</span>
                    <div className="text-white font-medium">{new Date(booking.startTime).toLocaleString()}</div>
                    <div className="text-[11px] text-slate-400">{booking.pickupHubName}</div>
                  </div>
                  <div>
                    <span className="text-slate-500">Return:</span>
                    <div className="text-white font-medium">{new Date(booking.endTime).toLocaleString()}</div>
                    <div className="text-[11px] text-slate-400">{booking.returnHubName}</div>
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs pt-2 border-t border-slate-800/80">
                  <div>
                    <span className="text-slate-500">Total Rental Paid:</span>
                    <div className="font-mono font-bold text-white text-sm">
                      ₹{Number(booking.totalRentalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {['AWAITING_PAYMENT', 'CONFIRMED'].includes(booking.status) && (
                      <button
                        onClick={() => setCancellingBooking(booking)}
                        className="px-3 py-1.5 rounded-lg border border-rose-500/30 text-rose-400 hover:bg-rose-950/30 text-xs font-semibold transition-colors"
                      >
                        Cancel Booking
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        /* Rental Fee Refunds Table */
        refunds.length === 0 ? (
          <div className="glass-card p-12 rounded-2xl text-center space-y-4">
            <ShieldCheck className="w-12 h-12 text-slate-600 mx-auto" />
            <h3 className="text-lg font-bold text-white">No rental refunds recorded</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Any partner rental fee refunds resulting from cancellations or adjustments will appear here with full audit traces.
            </p>
          </div>
        ) : (
          <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-900 border-b border-slate-800 text-[11px] text-slate-400 uppercase font-semibold">
                  <tr>
                    <th className="py-3 px-4">Refund Ref</th>
                    <th className="py-3 px-4">Booking / Reason</th>
                    <th className="py-3 px-4">Paid Gross</th>
                    <th className="py-3 px-4">Cancellation Fee</th>
                    <th className="py-3 px-4">Refund Amount</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Customer Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {refunds.map((r) => (
                    <tr key={r.id} className="hover:bg-slate-900/40">
                      <td className="py-3.5 px-4 font-mono font-bold text-white">
                        {r.refundReference}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-white">Booking #{r.bookingId}</div>
                        <div className="text-[11px] text-slate-400">{r.refundReason}</div>
                      </td>
                      <td className="py-3.5 px-4 font-mono">
                        ₹{Number(r.grossAmountPaid).toFixed(2)}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-rose-400">
                        -₹{Number(r.cancellationFeeDeducted).toFixed(2)}
                      </td>
                      <td className="py-3.5 px-4 font-mono font-bold text-emerald-400">
                        ₹{Number(r.refundAmount).toFixed(2)}
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={r.refundStatus} />
                        {r.partnerTransactionReference && (
                          <div className="text-[10px] text-slate-400 font-mono mt-0.5">
                            UTR: {r.partnerTransactionReference}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        {r.refundStatus === 'DISBURSED_BY_PARTNER' && (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => handleAcknowledgeRefund(r.id)}
                              className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-lg text-xs transition-colors"
                            >
                              Acknowledge
                            </button>
                            <button
                              onClick={() => setDisputingRefund(r)}
                              className="px-2.5 py-1 bg-slate-800 hover:bg-rose-900/60 border border-rose-500/40 text-rose-300 font-semibold rounded-lg text-xs transition-colors"
                            >
                              Dispute
                            </button>
                          </div>
                        )}
                        {r.refundStatus === 'CUSTOMER_ACKNOWLEDGED' && (
                          <span className="text-emerald-400 font-semibold text-[11px] flex items-center justify-end gap-1">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            <span>Confirmed</span>
                          </span>
                        )}
                        {r.refundStatus === 'DISPUTED_BY_CUSTOMER' && (
                          <span className="text-rose-400 font-semibold text-[11px]">
                            Disputed (Under Admin Review)
                          </span>
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

      {/* Cancel Booking Modal */}
      {cancellingBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 className="text-base font-bold text-white">Cancel Booking {cancellingBooking.bookingCode}</h3>
              <button onClick={() => setCancellingBooking(null)} className="text-slate-400 hover:text-white">✕</button>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Are you sure you want to cancel this booking? If eligible, the rental partner will disburse your refundable balance under our Safeguard B cancellation terms.
            </p>
            <form onSubmit={handleCancelBooking} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">Reason for Cancellation *</label>
                <textarea
                  required
                  rows={3}
                  placeholder="e.g. Change of travel plans to Hyderabad"
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setCancellingBooking(null)}
                  className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Keep Booking
                </button>
                <button
                  type="submit"
                  disabled={cancelSubmitting}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-xl transition-colors"
                >
                  {cancelSubmitting ? 'Cancelling...' : 'Confirm Cancellation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Dispute Refund Modal */}
      {disputingRefund && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 className="text-base font-bold text-white">Dispute Refund #{disputingRefund.refundReference}</h3>
              <button onClick={() => setDisputingRefund(null)} className="text-slate-400 hover:text-white">✕</button>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              If the partner's claimed UTR was not credited to your bank account or the amount is incorrect, state your reason below. This will trigger immediate Admin review.
            </p>
            <form onSubmit={handleDisputeRefund} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">Dispute Reason &amp; Bank Details *</label>
                <textarea
                  required
                  rows={3}
                  placeholder="e.g. Partner UTR 429182910 was not credited to my HDFC account. Net balance still pending."
                  value={disputeReason}
                  onChange={(e) => setDisputeReason(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setDisputingRefund(null)}
                  className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={disputeSubmitting}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-xl transition-colors"
                >
                  {disputeSubmitting ? 'Submitting Dispute...' : 'Escalate Dispute to Admin'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

export default CustomerDashboard;
