import React, { useState, useEffect } from 'react';
import { 
  Key, 
  Car, 
  FileCheck, 
  CheckCircle2, 
  DollarSign, 
  AlertCircle, 
  PlusCircle, 
  ArrowRight, 
  Clock, 
  TrendingUp,
  ShieldCheck,
  Building2,
  FileSignature
} from 'lucide-react';
import { contributorApi, locationApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';

export const ContributorDashboard = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('earnings'); // 'earnings' | 'vehicles' | 'agreements'
  const [vehicles, setVehicles] = useState([]);
  const [agreements, setAgreements] = useState([]);
  const [payables, setPayables] = useState([]);
  const [hubs, setHubs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  // New Application Modal
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [appMake, setAppMake] = useState('Tata');
  const [appModel, setAppModel] = useState('Nexon EV');
  const [appYear, setAppYear] = useState('2024');
  const [appPlate, setAppPlate] = useState('AP 16 CZ 9988');
  const [appHubId, setAppHubId] = useState('');
  const [applySubmitting, setApplySubmitting] = useState(false);

  // Digital Sign Modal
  const [signingAgreement, setSigningAgreement] = useState(null);
  const [digitalSignConsent, setDigitalSignConsent] = useState(false);
  const [signSubmitting, setSignSubmitting] = useState(false);

  // Dispute Payable Modal
  const [disputingPayable, setDisputingPayable] = useState(null);
  const [disputeReason, setDisputeReason] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      const [vRes, aRes, pRes, hRes] = await Promise.all([
        contributorApi.getMyVehicles(),
        contributorApi.getMyAgreements(),
        contributorApi.getMyPayables(),
        locationApi.getAllHubs()
      ]);
      setVehicles(vRes.data?.data || []);
      setAgreements(aRes.data?.data || []);
      setPayables(pRes.data?.data || []);
      setHubs(hRes.data?.data || []);
      if (hRes.data?.data?.length > 0) {
        setAppHubId(hRes.data.data[0].id);
      }
    } catch (err) {
      console.error(err);
      setError('Could not load contributor dashboard data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApplyVehicle = async (e) => {
    e.preventDefault();
    setApplySubmitting(true);
    try {
      await contributorApi.submitVehicleApplication({
        make: appMake,
        model: appModel,
        year: parseInt(appYear),
        registrationPlateNumber: appPlate,
        preferredHubId: parseInt(appHubId)
      });
      setMessage('Vehicle application submitted! Rental partner review underway.');
      setShowApplyModal(false);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Application submission failed.');
    } finally {
      setApplySubmitting(false);
    }
  };

  const handleSignAgreement = async (e) => {
    e.preventDefault();
    if (!signingAgreement || !digitalSignConsent) return;
    setSignSubmitting(true);
    try {
      await contributorApi.signAgreement(signingAgreement.id, {
        signedByLegalName: user?.fullName || 'Contributor Legal Signer',
        consentAccepted: true
      });
      setMessage(`Agreement #${signingAgreement.id} digitally executed! Vehicle is now active in fleet.`);
      setSigningAgreement(null);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Agreement signing failed.');
    } finally {
      setSignSubmitting(false);
    }
  };

  const handleConfirmPayable = async (payableId) => {
    try {
      await contributorApi.acknowledgeSettlement(payableId);
      setMessage(`Settlement confirmed! Status updated to CONFIRMED.`);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Confirmation failed.');
    }
  };

  const handleDisputePayable = async (e) => {
    e.preventDefault();
    if (!disputingPayable || !disputeReason.trim()) return;
    try {
      await contributorApi.disputeSettlement(disputingPayable.id, disputeReason.trim());
      setMessage(`Payable disputed. Notified Admin and Partner.`);
      setDisputingPayable(null);
      setDisputeReason('');
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Dispute submission failed.');
    }
  };

  const totalEarnings = payables
    .filter(p => ['SETTLED_BY_PARTNER', 'CONFIRMED_BY_CONTRIBUTOR'].includes(p.payableStatus))
    .reduce((sum, p) => sum + parseFloat(p.agreedCutAmount || 0), 0);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-teal-400">
            Contributor Portal
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white mt-1">
            {user?.fullName || 'Car Owner'} &bull; Fleet Monetization
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Attach vehicles to certified Vijayawada partners, monitor revenue splits, and track direct bank payouts.
          </p>
        </div>

        <button
          onClick={() => setShowApplyModal(true)}
          className="px-4 py-2.5 bg-teal-600 hover:bg-teal-500 text-slate-950 font-bold text-xs rounded-xl shadow-md transition-all flex items-center gap-1.5 self-start sm:self-auto"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Attach Another Car</span>
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="text-xs text-slate-400 uppercase font-semibold flex items-center justify-between">
            <span>Cumulative Earnings Settled</span>
            <DollarSign className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-extrabold text-emerald-400 font-mono">
            ₹{totalEarnings.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <p className="text-[11px] text-slate-500">Transferred directly to your UPI bank account</p>
        </div>

        <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="text-xs text-slate-400 uppercase font-semibold flex items-center justify-between">
            <span>Attached Vehicles</span>
            <Car className="w-4 h-4 text-brand-400" />
          </div>
          <div className="text-2xl font-extrabold text-white font-mono">
            {vehicles.length} Active
          </div>
          <p className="text-[11px] text-slate-500">Stationed across Vijayawada hubs</p>
        </div>

        <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="text-xs text-slate-400 uppercase font-semibold flex items-center justify-between">
            <span>Signed Agreements</span>
            <FileCheck className="w-4 h-4 text-teal-400" />
          </div>
          <div className="text-2xl font-extrabold text-teal-300 font-mono">
            {agreements.length} Active
          </div>
          <p className="text-[11px] text-slate-500">Guaranteed contractual revenue split</p>
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
      <div className="flex gap-2 border-b border-slate-800 pb-2 text-xs font-semibold">
        <button
          onClick={() => setActiveTab('earnings')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'earnings'
              ? 'bg-teal-600 text-slate-950 font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Payables &amp; Payouts ({payables.length})
        </button>
        <button
          onClick={() => setActiveTab('vehicles')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'vehicles'
              ? 'bg-teal-600 text-slate-950 font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Attached Fleet ({vehicles.length})
        </button>
        <button
          onClick={() => setActiveTab('agreements')}
          className={`px-4 py-2 rounded-xl transition-all ${
            activeTab === 'agreements'
              ? 'bg-teal-600 text-slate-950 font-bold shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          Agreements &amp; Contracts ({agreements.length})
        </button>
      </div>

      {loading ? (
        <div className="min-h-[40vh] flex items-center justify-center">
          <div className="w-10 h-10 border-4 border-teal-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : activeTab === 'earnings' ? (
        /* Payables Table */
        payables.length === 0 ? (
          <div className="glass-card p-12 rounded-2xl text-center space-y-4">
            <DollarSign className="w-12 h-12 text-slate-600 mx-auto" />
            <h3 className="text-lg font-bold text-white">No earnings accrued yet</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Completed customer bookings on your attached vehicles will automatically generate monthly payables here.
            </p>
          </div>
        ) : (
          <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-900 border-b border-slate-800 text-[11px] text-slate-400 uppercase font-semibold">
                  <tr>
                    <th className="py-3 px-4">Payable Ref</th>
                    <th className="py-3 px-4">Booking / Partner</th>
                    <th className="py-3 px-4">Agreed Cut (₹)</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Partner UTR</th>
                    <th className="py-3 px-4 text-right">Contributor Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {payables.map((p) => (
                    <tr key={p.id} className="hover:bg-slate-900/40">
                      <td className="py-3.5 px-4 font-mono font-bold text-white">#{p.id}</td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-white">{p.bookingCode}</div>
                        <div className="text-[11px] text-slate-400">{p.partnerBusinessName}</div>
                      </td>
                      <td className="py-3.5 px-4 font-mono font-bold text-emerald-400 text-sm">
                        ₹{Number(p.agreedCutAmount).toFixed(2)}
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={p.payableStatus} />
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-300">
                        {p.partnerTransactionReference || '—'}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        {p.payableStatus === 'SETTLED_BY_PARTNER' && (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => handleConfirmPayable(p.id)}
                              className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-lg text-xs transition-colors"
                            >
                              Confirm Received
                            </button>
                            <button
                              onClick={() => setDisputingPayable(p)}
                              className="px-2.5 py-1 bg-slate-800 hover:bg-rose-900/60 border border-rose-500/40 text-rose-300 font-semibold rounded-lg text-xs transition-colors"
                            >
                              Dispute
                            </button>
                          </div>
                        )}
                        {p.payableStatus === 'CONFIRMED_BY_CONTRIBUTOR' && (
                          <span className="text-emerald-400 font-semibold text-[11px] flex items-center justify-end gap-1">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            <span>Confirmed &bull; Reconciled</span>
                          </span>
                        )}
                        {p.payableStatus === 'DISPUTED_BY_CONTRIBUTOR' && (
                          <span className="text-rose-400 font-semibold text-[11px]">
                            Disputed (Under Review)
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
      ) : activeTab === 'vehicles' ? (
        /* Attached Fleet */
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {vehicles.map((v) => (
            <div key={v.id} className="glass-card rounded-2xl overflow-hidden p-5 space-y-4">
              <div className="flex items-start justify-between">
                <div>
                  <h4 className="text-base font-bold text-white">{v.make} {v.model}</h4>
                  <p className="text-xs text-slate-400 font-mono">AP {v.plateNumber}</p>
                </div>
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-800 text-teal-300">
                  {v.status}
                </span>
              </div>

              <div className="text-xs text-slate-400 space-y-1">
                <div>Partner: <span className="text-white font-medium">{v.partnerBusinessName}</span></div>
                <div>Stationed at: <span className="text-slate-300">{v.partnerHubName}</span></div>
                <div>Custody: <span className="text-brand-300 font-mono">{v.custodyState}</span></div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* Agreements Table */
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {agreements.map((a) => (
              <div key={a.id} className="glass-card p-6 rounded-2xl border border-slate-800 space-y-4">
                <div className="flex items-start justify-between">
                  <div>
                    <span className="text-[10px] font-mono text-slate-500 uppercase">
                      Contract Ref: AGR-{a.id}
                    </span>
                    <h4 className="text-base font-bold text-white mt-0.5">
                      {a.partnerBusinessName}
                    </h4>
                    <p className="text-xs text-slate-400">Attached Vehicle: {a.vehicleModel || 'Tata Nexon EV'}</p>
                  </div>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-800 text-teal-300">
                    {a.status}
                  </span>
                </div>

                <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 space-y-1.5 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Agreed Contributor Cut:</span>
                    <span className="font-mono font-bold text-emerald-400 text-sm">
                      {a.contributorCutPercentage || '70.0'}% of Rental Fees
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Effective Window:</span>
                    <span className="text-slate-300">12 Months (Renewable)</span>
                  </div>
                </div>

                {a.status === 'DRAFT' && (
                  <button
                    onClick={() => setSigningAgreement(a)}
                    className="w-full py-2.5 bg-teal-600 hover:bg-teal-500 text-slate-950 font-bold rounded-xl text-xs transition-colors flex items-center justify-center gap-1.5"
                  >
                    <FileSignature className="w-4 h-4" />
                    <span>Review &amp; Sign Agreement Digitally</span>
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Apply Car Modal */}
      {showApplyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 className="text-base font-bold text-white">Attach Vehicle to Vijayawada Fleet</h3>
              <button onClick={() => setShowApplyModal(false)} className="text-slate-400 hover:text-white">✕</button>
            </div>
            <form onSubmit={handleApplyVehicle} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-semibold">Make *</label>
                  <input
                    type="text"
                    required
                    value={appMake}
                    onChange={(e) => setAppMake(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                  />
                </div>
                <div>
                  <label className="text-slate-300 font-semibold">Model *</label>
                  <input
                    type="text"
                    required
                    value={appModel}
                    onChange={(e) => setAppModel(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-semibold">Year *</label>
                  <input
                    type="number"
                    required
                    value={appYear}
                    onChange={(e) => setAppYear(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono"
                  />
                </div>
                <div>
                  <label className="text-slate-300 font-semibold">Plate Number *</label>
                  <input
                    type="text"
                    required
                    value={appPlate}
                    onChange={(e) => setAppPlate(e.target.value)}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="text-slate-300 font-semibold">Preferred Vijayawada Hub *</label>
                <select
                  value={appHubId}
                  onChange={(e) => setAppHubId(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white"
                >
                  {hubs.map((h) => (
                    <option key={h.id} value={h.id}>{h.hubName}</option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowApplyModal(false)}
                  className="px-4 py-2 text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={applySubmitting}
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-500 text-slate-950 font-bold text-xs rounded-xl"
                >
                  {applySubmitting ? 'Submitting...' : 'Submit Application'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Digital Sign Modal */}
      {signingAgreement && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-white">Execute Contribution Agreement #{signingAgreement.id}</h3>
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 text-xs text-slate-300 max-h-48 overflow-y-auto space-y-2">
              <p><strong>1. PARTIES:</strong> Contributor ({user?.fullName}) and Managing Partner ({signingAgreement.partnerBusinessName}).</p>
              <p><strong>2. VEHICLE:</strong> Registered in Andhra Pradesh, inspected and insured for self-drive commercial rental operations.</p>
              <p><strong>3. REVENUE SHARE:</strong> Contributor receives 70% of gross realized rental proceeds for completed trips, payable directly via UPI within 5 business days.</p>
              <p><strong>4. SAFABILITY &amp; CUSTODY:</strong> Partner assumes operational custody during rental cycles and maintains vehicle condition records.</p>
            </div>

            <form onSubmit={handleSignAgreement} className="space-y-4">
              <label className="flex items-start gap-2.5 text-xs text-slate-300 cursor-pointer">
                <input
                  type="checkbox"
                  required
                  checked={digitalSignConsent}
                  onChange={(e) => setDigitalSignConsent(e.target.checked)}
                  className="mt-0.5 rounded border-slate-700 bg-slate-950 text-teal-500 focus:ring-0"
                />
                <span>I confirm that I am the legal owner of this vehicle and grant consent to partner operations under these terms.</span>
              </label>

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSigningAgreement(null)}
                  className="px-4 py-2 text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={signSubmitting || !digitalSignConsent}
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-500 disabled:bg-slate-800 text-slate-950 font-bold text-xs rounded-xl"
                >
                  {signSubmitting ? 'Signing...' : 'Sign & Activate Fleet'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Dispute Payable Modal */}
      {disputingPayable && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-white">Dispute Payout #{disputingPayable.id}</h3>
            <form onSubmit={handleDisputePayable} className="space-y-3">
              <label className="text-xs text-slate-300 font-semibold">Dispute Reason *</label>
              <textarea
                required
                rows={3}
                placeholder="e.g. Partner UTR not credited or agreed cut percentage discrepancy"
                value={disputeReason}
                onChange={(e) => setDisputeReason(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setDisputingPayable(null)}
                  className="px-4 py-2 text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-xl"
                >
                  Submit Dispute
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

export default ContributorDashboard;
