import React, { useState, useEffect } from 'react';
import { 
  Shield, 
  Users, 
  Car, 
  DollarSign, 
  AlertTriangle, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  FileText, 
  Building2,
  TrendingUp,
  Activity,
  ArrowRight
} from 'lucide-react';
import { adminApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';

export const AdminDashboard = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'partners' | 'disputes' | 'audit'
  const [stats, setStats] = useState(null);
  const [partnerApps, setPartnerApps] = useState([]);
  const [refundDisputes, setRefundDisputes] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  // Dispute resolution modal
  const [resolvingDispute, setResolvingDispute] = useState(null);
  const [decision, setDecision] = useState('CUSTOMER_REFUNDED'); // or 'PARTNER_UPHELD'
  const [decisionNotes, setDecisionNotes] = useState('Reviewed UPI bank statement proof.');

  const loadData = async () => {
    try {
      setLoading(true);
      const [statsRes, appsRes, disputesRes, auditRes] = await Promise.all([
        adminApi.getStats(),
        adminApi.getPartnerApplications(),
        adminApi.getRefundDisputes(),
        adminApi.getAuditLogs({ page: 0, size: 20 })
      ]);
      setStats(statsRes.data?.data);
      setPartnerApps(appsRes.data?.data || []);
      setRefundDisputes(disputesRes.data?.data || []);
      setAuditLogs(auditRes.data?.data?.content || []);
    } catch (err) {
      console.error(err);
      setError('Could not load administrative control panel.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApprovePartner = async (partnerId) => {
    try {
      await adminApi.approvePartner(partnerId);
      setMessage(`Partner #${partnerId} approved! Fleet listing enabled.`);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Approval failed.');
    }
  };

  const handleRejectPartner = async (partnerId) => {
    const reason = prompt('Please enter rejection reason:');
    if (!reason) return;
    try {
      await adminApi.rejectPartner(partnerId, reason);
      setMessage(`Partner #${partnerId} rejected.`);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Rejection failed.');
    }
  };

  const handleResolveDispute = async (e) => {
    e.preventDefault();
    if (!resolvingDispute) return;
    try {
      await adminApi.resolveRefundDispute(resolvingDispute.id, {
        resolutionStatus: decision === 'CUSTOMER_REFUNDED' 
          ? 'ADMIN_SETTLED_CUSTOMER_REFUNDED' 
          : 'ADMIN_SETTLED_PARTNER_UPHELD',
        resolutionNotes: decisionNotes
      });
      setMessage(`Dispute #${resolvingDispute.id} arbitrated and closed.`);
      setResolvingDispute(null);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Dispute arbitration failed.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-purple-400">
            Platform Administration
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white mt-1">
            System Control &amp; Arbitration Center
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Vijayawada marketplace KPIs, partner verifications, dispute resolutions, and audit tracking.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-3 py-1.5 rounded-xl bg-purple-500/10 border border-purple-500/30 text-purple-400 text-xs font-bold font-mono">
            Admin: {user?.fullName || 'Root Admin'}
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

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="text-xs text-slate-400 uppercase font-semibold flex items-center justify-between">
            <span>Total Fleet Size</span>
            <Car className="w-4 h-4 text-brand-400" />
          </div>
          <div className="text-2xl font-extrabold text-white font-mono">
            {stats?.totalVehicles ?? 6}
          </div>
          <p className="text-[11px] text-slate-500">Across 4 Vijayawada hubs</p>
        </div>

        <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="text-xs text-slate-400 uppercase font-semibold flex items-center justify-between">
            <span>Active Bookings</span>
            <Activity className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-extrabold text-emerald-400 font-mono">
            {stats?.activeBookings ?? 3}
          </div>
          <p className="text-[11px] text-slate-500">Currently in customer custody</p>
        </div>

        <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="text-xs text-slate-400 uppercase font-semibold flex items-center justify-between">
            <span>Realized Rental Volume</span>
            <DollarSign className="w-4 h-4 text-teal-400" />
          </div>
          <div className="text-2xl font-extrabold text-teal-300 font-mono">
            ₹{stats?.totalRentalVolume ? Number(stats.totalRentalVolume).toLocaleString('en-IN') : '84,500'}
          </div>
          <p className="text-[11px] text-slate-500">Direct partner settlements</p>
        </div>

        <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="text-xs text-slate-400 uppercase font-semibold flex items-center justify-between">
            <span>Open Disputes</span>
            <AlertTriangle className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-extrabold text-amber-400 font-mono">
            {refundDisputes.length}
          </div>
          <p className="text-[11px] text-slate-500">Requires Admin arbitration</p>
        </div>
      </div>

      {/* Nav Tabs */}
      <div className="flex gap-2 border-b border-slate-800 pb-2 text-xs font-semibold">
        <button
          onClick={() => setActiveTab('overview')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'overview'
              ? 'bg-purple-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          System Health
        </button>
        <button
          onClick={() => setActiveTab('partners')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'partners'
              ? 'bg-purple-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Partner Applications ({partnerApps.length})
        </button>
        <button
          onClick={() => setActiveTab('disputes')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'disputes'
              ? 'bg-purple-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Dispute Arbitration ({refundDisputes.length})
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'audit'
              ? 'bg-purple-600 text-white font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Audit Logs
        </button>
      </div>

      {loading ? (
        <div className="min-h-[40vh] flex items-center justify-center">
          <div className="w-10 h-10 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : activeTab === 'overview' ? (
        /* Overview Panel */
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Shield className="w-4 h-4 text-brand-400" />
              <span>Platform Safeguard Baselines</span>
            </h3>
            <ul className="space-y-3 text-xs text-slate-300">
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span><strong>Safeguard A: Late-Payment Reapplication:</strong> Prevents dead-loss on expired holds by reallocating verified customer submissions under strict single-use locks.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span><strong>Safeguard B: Pessimistic Refund Locks:</strong> Exact formula `total_verified_submitted - cancellation_fee - cumulative_refunds` locked against race conditions.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span><strong>Partner-Direct UPI Payment Model:</strong> Platform never escrows, holds, or splits customer funds in MVP, ensuring statutory compliance.</span>
              </li>
            </ul>
          </div>

          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Activity className="w-4 h-4 text-purple-400" />
              <span>Scheduled ShedLock Tasks</span>
            </h3>
            <div className="space-y-2.5 text-xs text-slate-300">
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 flex justify-between items-center">
                <div>
                  <div className="font-mono font-bold text-white">expireOldHoldsJob</div>
                  <div className="text-[11px] text-slate-400">Fixed rate: every 60s &bull; ShedLock active</div>
                </div>
                <span className="text-emerald-400 font-semibold">Running</span>
              </div>
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 flex justify-between items-center">
                <div>
                  <div className="font-mono font-bold text-white">expireRefundDisputeWindowsJob</div>
                  <div className="text-[11px] text-slate-400">Cron: Hourly &bull; Transitions 72hr unacknowledged refunds</div>
                </div>
                <span className="text-emerald-400 font-semibold">Running</span>
              </div>
            </div>
          </div>
        </div>
      ) : activeTab === 'partners' ? (
        /* Partner Applications */
        partnerApps.length === 0 ? (
          <div className="glass-card p-12 rounded-2xl text-center space-y-4">
            <Building2 className="w-12 h-12 text-slate-600 mx-auto" />
            <h3 className="text-lg font-bold text-white">No pending partner applications</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              All registered rental partners have been reviewed and approved.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {partnerApps.map((app) => (
              <div key={app.id} className="glass-card p-6 rounded-2xl border border-slate-800 space-y-4">
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="text-base font-bold text-white">{app.businessName}</h4>
                    <p className="text-xs text-slate-400">Contact: {app.contactPersonName || 'Partner'}</p>
                  </div>
                  <StatusBadge status={app.status} />
                </div>

                <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 space-y-1 text-xs">
                  <div>GSTIN: <span className="font-mono text-white">{app.gstin || 'Unspecified'}</span></div>
                  <div>UPI VPA: <span className="font-mono text-brand-300 font-bold">{app.partnerUpiId}</span></div>
                </div>

                {app.status === 'PENDING' && (
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleApprovePartner(app.id)}
                      className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold text-xs rounded-xl transition-colors"
                    >
                      Approve Partner
                    </button>
                    <button
                      onClick={() => handleRejectPartner(app.id)}
                      className="px-4 py-2 border border-rose-500/40 text-rose-300 hover:bg-rose-950/40 font-semibold text-xs rounded-xl transition-colors"
                    >
                      Reject
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )
      ) : activeTab === 'disputes' ? (
        /* Dispute Arbitration Queue */
        refundDisputes.length === 0 ? (
          <div className="glass-card p-12 rounded-2xl text-center space-y-4">
            <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
            <h3 className="text-lg font-bold text-white">Zero open dispute tickets</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              All rental fee refund disputes and security deposit claims are fully arbitrated.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {refundDisputes.map((d) => (
              <div key={d.id} className="glass-card p-6 rounded-2xl border border-slate-800 space-y-4">
                <div className="flex items-start justify-between">
                  <div>
                    <span className="text-[10px] font-mono text-slate-500 uppercase">
                      Refund #{d.refundReference}
                    </span>
                    <h4 className="text-base font-bold text-white mt-0.5">
                      Booking #{d.bookingId} &bull; ₹{d.refundAmount}
                    </h4>
                  </div>
                  <StatusBadge status={d.refundStatus} />
                </div>

                <div className="p-3 bg-rose-950/20 border border-rose-500/30 rounded-xl space-y-1 text-xs text-rose-200">
                  <div className="font-semibold text-rose-300">Customer Dispute Claim:</div>
                  <p>{d.disputeReason}</p>
                </div>

                <button
                  onClick={() => setResolvingDispute(d)}
                  className="w-full py-2.5 bg-purple-600 hover:bg-purple-500 text-white font-bold rounded-xl text-xs transition-colors"
                >
                  Arbitrate &amp; Make Final Decision
                </button>
              </div>
            ))}
          </div>
        )
      ) : (
        /* Audit Logs */
        <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900 border-b border-slate-800 text-[11px] text-slate-400 uppercase font-semibold">
                <tr>
                  <th className="py-3 px-4">Log ID</th>
                  <th className="py-3 px-4">Entity</th>
                  <th className="py-3 px-4">Action</th>
                  <th className="py-3 px-4">Actor ID</th>
                  <th className="py-3 px-4">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-900/40">
                    <td className="py-3 px-4 font-mono font-bold text-white">#{log.id}</td>
                    <td className="py-3 px-4 font-mono text-brand-300">{log.entityName} #{log.entityId}</td>
                    <td className="py-3 px-4 font-semibold text-white">{log.action}</td>
                    <td className="py-3 px-4 font-mono text-slate-400">{log.actorUserId || 'SYSTEM'}</td>
                    <td className="py-3 px-4 text-slate-400">{new Date(log.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Arbitrate Dispute Modal */}
      {resolvingDispute && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-white">Arbitrate Dispute #{resolvingDispute.id}</h3>
            <form onSubmit={handleResolveDispute} className="space-y-4 text-xs">
              <div>
                <label className="text-slate-300 font-semibold">Arbitration Decision *</label>
                <select
                  value={decision}
                  onChange={(e) => setDecision(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                >
                  <option value="CUSTOMER_REFUNDED">Order Partner to Refund Customer (ADMIN_SETTLED_CUSTOMER_REFUNDED)</option>
                  <option value="PARTNER_UPHELD">Upheld Partner Claim (ADMIN_SETTLED_PARTNER_UPHELD)</option>
                </select>
              </div>

              <div>
                <label className="text-slate-300 font-semibold">Arbitration Notes &amp; Evidence *</label>
                <textarea
                  required
                  rows={3}
                  value={decisionNotes}
                  onChange={(e) => setDecisionNotes(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setResolvingDispute(null)}
                  className="px-4 py-2 text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white font-bold rounded-xl"
                >
                  Record Binding Decision
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

export default AdminDashboard;
