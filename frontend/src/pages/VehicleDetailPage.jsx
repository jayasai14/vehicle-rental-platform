import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  Car, 
  MapPin, 
  Calendar, 
  ShieldCheck, 
  CheckCircle, 
  Fuel, 
  Gauge, 
  Users, 
  Sparkles, 
  Clock, 
  Building2, 
  AlertCircle,
  ArrowRight,
  Info
} from 'lucide-react';
import { vehicleApi, bookingApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';

export const VehicleDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  const [vehicle, setVehicle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [creatingHold, setCreatingHold] = useState(false);

  // Rental duration state
  const [startTime, setStartTime] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    d.setHours(9, 0, 0, 0);
    return d.toISOString().slice(0, 16);
  });

  const [endTime, setEndTime] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 3);
    d.setHours(18, 0, 0, 0);
    return d.toISOString().slice(0, 16);
  });

  useEffect(() => {
    vehicleApi.getVehicleDetails(id)
      .then(res => {
        setVehicle(res.data?.data);
      })
      .catch(err => {
        setError('Vehicle not found or inactive');
      })
      .finally(() => setLoading(false));
  }, [id]);

  // Calculate rental cost
  const calculateDays = () => {
    if (!startTime || !endTime) return 1;
    const start = new Date(startTime).getTime();
    const end = new Date(endTime).getTime();
    const diffHours = Math.max(1, (end - start) / (1000 * 60 * 60));
    return Math.max(1, Math.ceil(diffHours / 24));
  };

  const days = calculateDays();
  const dailyRate = vehicle?.dailyRate ? parseFloat(vehicle.dailyRate) : 2200;
  const rentalAmount = dailyRate * days;
  const depositAmount = vehicle?.securityDepositAmount ? parseFloat(vehicle.securityDepositAmount) : 5000;
  const totalDueAtCheckout = rentalAmount; // Under MVP model, rental fee is demanded upfront; deposit collected by partner prior to handover

  const handleCreateHold = async () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/vehicles/${id}` } });
      return;
    }

    setCreatingHold(true);
    setError('');

    try {
      const payload = {
        vehicleId: parseInt(id),
        startTime: startTime + ':00Z',
        endTime: endTime + ':00Z',
        pickupLocationId: vehicle?.partnerHubId || 1,
        returnLocationId: vehicle?.partnerHubId || 1,
      };

      const res = await bookingApi.createHold(payload);
      const hold = res.data?.data;
      navigate(`/checkout/${hold.id}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not place hold. Vehicle may be booked for selected dates.');
    } finally {
      setCreatingHold(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error && !vehicle) {
    return (
      <div className="max-w-xl mx-auto py-20 text-center space-y-4">
        <AlertCircle className="w-12 h-12 text-rose-400 mx-auto" />
        <h2 className="text-xl font-bold text-white">Vehicle Unavailable</h2>
        <p className="text-sm text-slate-400">{error}</p>
        <Link to="/vehicles" className="inline-block px-4 py-2 bg-brand-600 text-slate-950 font-bold rounded-xl text-xs">
          Browse Other Cars
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-10">
      
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-xs text-slate-400">
        <Link to="/" className="hover:text-white">Home</Link>
        <span>/</span>
        <Link to="/vehicles" className="hover:text-white">Fleet</Link>
        <span>/</span>
        <span className="text-brand-400 font-medium">{vehicle.make} {vehicle.model}</span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        {/* Main Details (2 cols) */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Main Visual Image Banner */}
          <div className="relative rounded-3xl overflow-hidden bg-slate-900 border border-slate-800 shadow-2xl h-80 sm:h-96">
            <img
              src={vehicle.primaryImageUrl || 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=1200&q=80'}
              alt={`${vehicle.make} ${vehicle.model}`}
              className="w-full h-full object-cover"
            />
            <div className="absolute top-4 left-4 flex gap-2">
              <span className="px-3 py-1 rounded-full bg-slate-950/80 backdrop-blur-md border border-slate-700 text-xs font-bold text-white">
                {vehicle.category}
              </span>
              <span className="px-3 py-1 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-xs font-bold text-emerald-300">
                Verified Partner Fleet
              </span>
            </div>
          </div>

          {/* Title & Core Highlights */}
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h1 className="text-3xl sm:text-4xl font-extrabold text-white">
                  {vehicle.make} {vehicle.model}
                </h1>
                <p className="text-sm text-slate-400 mt-1 flex items-center gap-1.5">
                  <MapPin className="w-4 h-4 text-brand-400" />
                  <span>Stationed at: <strong>{vehicle.partnerHubName || 'Benz Circle Central Hub'}</strong>, Vijayawada</span>
                </p>
              </div>

              <div className="text-left sm:text-right">
                <div className="text-xs text-slate-400">Daily Base Rate</div>
                <div className="text-3xl font-extrabold text-brand-400 font-mono">
                  ₹{Number(dailyRate).toLocaleString('en-IN')}
                  <span className="text-sm font-normal text-slate-400">/day</span>
                </div>
              </div>
            </div>

            {/* Spec Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
              <div className="p-3 bg-slate-900/80 rounded-xl border border-slate-800 flex items-center gap-3">
                <Gauge className="w-5 h-5 text-brand-400" />
                <div>
                  <div className="text-[10px] text-slate-500 uppercase font-semibold">Transmission</div>
                  <div className="text-xs font-bold text-white">{vehicle.transmission}</div>
                </div>
              </div>

              <div className="p-3 bg-slate-900/80 rounded-xl border border-slate-800 flex items-center gap-3">
                <Fuel className="w-5 h-5 text-teal-400" />
                <div>
                  <div className="text-[10px] text-slate-500 uppercase font-semibold">Fuel</div>
                  <div className="text-xs font-bold text-white">{vehicle.fuelType}</div>
                </div>
              </div>

              <div className="p-3 bg-slate-900/80 rounded-xl border border-slate-800 flex items-center gap-3">
                <Users className="w-5 h-5 text-blue-400" />
                <div>
                  <div className="text-[10px] text-slate-500 uppercase font-semibold">Seating</div>
                  <div className="text-xs font-bold text-white">{vehicle.seatingCapacity || 5} Seats</div>
                </div>
              </div>

              <div className="p-3 bg-slate-900/80 rounded-xl border border-slate-800 flex items-center gap-3">
                <Sparkles className="w-5 h-5 text-amber-400" />
                <div>
                  <div className="text-[10px] text-slate-500 uppercase font-semibold">Model Year</div>
                  <div className="text-xs font-bold text-white">{vehicle.year}</div>
                </div>
              </div>
            </div>
          </div>

          {/* Pricing Policy Breakdown */}
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Info className="w-4 h-4 text-brand-400" />
              <span>Transparent Pricing &amp; Mileage Allowances</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800/80 space-y-1">
                <div className="text-slate-400">Included Distance</div>
                <div className="text-sm font-bold text-white font-mono">
                  {vehicle.freeKmPerDay || 250} km / day
                </div>
                <p className="text-[11px] text-slate-500">Generous limit for AP travel</p>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800/80 space-y-1">
                <div className="text-slate-400">Excess Distance Fee</div>
                <div className="text-sm font-bold text-white font-mono">
                  ₹{vehicle.excessKmRate || '12.00'} / km
                </div>
                <p className="text-[11px] text-slate-500">Settled directly at return</p>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800/80 space-y-1">
                <div className="text-slate-400">Security Deposit</div>
                <div className="text-sm font-bold text-emerald-400 font-mono">
                  ₹{Number(depositAmount).toLocaleString('en-IN')}
                </div>
                <p className="text-[11px] text-slate-500">100% Refundable on safe return</p>
              </div>
            </div>
          </div>

          {/* Managing Partner Card */}
          <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-brand-500/10 text-brand-400 flex items-center justify-center font-bold text-lg">
                <Building2 className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[11px] text-brand-400 uppercase font-semibold">Vehicle Managed By</span>
                <h4 className="text-sm font-bold text-white">{vehicle.partnerBusinessName || 'Sri Krishna Rentals'}</h4>
                <p className="text-xs text-slate-400">Certified Vijayawada Fleet Provider &bull; Direct UPI Payee</p>
              </div>
            </div>

            <div className="text-right">
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-semibold border border-emerald-500/20">
                <CheckCircle className="w-3.5 h-3.5" />
                <span>Verified Partner</span>
              </span>
            </div>
          </div>

        </div>

        {/* Reservation Sidebar (1 col) */}
        <div className="space-y-6">
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-6 lg:sticky lg:top-28 shadow-xl">
            
            <div className="border-b border-slate-800 pb-4">
              <h3 className="text-lg font-bold text-white">Book This Vehicle</h3>
              <p className="text-xs text-slate-400 mt-1">
                Creates an exclusive 15-minute lock while you pay via partner UPI.
              </p>
            </div>

            {error && (
              <div className="p-3 rounded-xl bg-rose-950/40 border border-rose-500/30 text-xs text-rose-300 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div className="space-y-4">
              {/* Start Date */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-brand-400" />
                  <span>Pickup Time</span>
                </label>
                <input
                  type="datetime-local"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
                />
              </div>

              {/* End Date */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-brand-400" />
                  <span>Dropoff Time</span>
                </label>
                <input
                  type="datetime-local"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
                />
              </div>
            </div>

            {/* Bill Summary */}
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800/80 space-y-2.5 text-xs">
              <div className="flex justify-between text-slate-400">
                <span>Duration ({days} {days === 1 ? 'day' : 'days'}):</span>
                <span className="text-white font-mono">₹{dailyRate.toFixed(2)} &times; {days}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Rental Fee Subtotal:</span>
                <span className="text-white font-mono font-semibold">₹{rentalAmount.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Platform Convenience Fee:</span>
                <span className="text-emerald-400 font-bold">₹0.00 (FREE)</span>
              </div>
              <div className="pt-2 border-t border-slate-800 flex justify-between items-center text-sm">
                <span className="font-bold text-white">Upfront Demand:</span>
                <span className="text-xl font-bold text-emerald-400 font-mono">
                  ₹{rentalAmount.toFixed(2)}
                </span>
              </div>
            </div>

            {/* Hold Button */}
            <button
              onClick={handleCreateHold}
              disabled={creatingHold}
              className="w-full py-3 bg-brand-600 hover:bg-brand-500 disabled:bg-slate-800 disabled:text-slate-500 text-slate-950 font-bold rounded-xl shadow-lg shadow-brand-500/20 transition-all flex items-center justify-center gap-2"
            >
              {creatingHold ? (
                <span>Locking Vehicle...</span>
              ) : (
                <>
                  <Clock className="w-4 h-4" />
                  <span>Create 15-Min Hold &amp; Pay</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>

            <div className="text-[11px] text-slate-400 text-center space-y-1">
              <p>Locks vehicle exclusively for you on the calendar.</p>
              <p className="text-brand-400">Zero platform escrow &bull; Direct partner settlement</p>
            </div>

          </div>
        </div>

      </div>

    </div>
  );
};

export default VehicleDetailPage;
