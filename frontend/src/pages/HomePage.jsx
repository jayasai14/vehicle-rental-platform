import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Search, 
  MapPin, 
  Calendar, 
  ShieldCheck, 
  Zap, 
  Clock, 
  Car, 
  CheckCircle2, 
  Star, 
  ArrowRight,
  TrendingUp,
  Building,
  ChevronRight
} from 'lucide-react';
import { vehicleApi, locationApi } from '../api/endpoints';

export const HomePage = () => {
  const navigate = useNavigate();
  const [hubs, setHubs] = useState([]);
  const [featuredVehicles, setFeaturedVehicles] = useState([]);
  const [selectedHub, setSelectedHub] = useState('');
  const [category, setCategory] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  useEffect(() => {
    // Default dates: tomorrow to day after tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dayAfter = new Date();
    dayAfter.setDate(dayAfter.getDate() + 3);

    setStartDate(tomorrow.toISOString().split('T')[0] + 'T09:00');
    setEndDate(dayAfter.toISOString().split('T')[0] + 'T18:00');

    // Fetch hubs & featured vehicles
    locationApi.getAllHubs()
      .then(res => setHubs(res.data?.data || []))
      .catch(err => console.error('Failed to load hubs:', err));

    vehicleApi.searchVehicles({ page: 0, size: 6 })
      .then(res => setFeaturedVehicles(res.data?.data?.content || []))
      .catch(err => console.error('Failed to load featured vehicles:', err));
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    const query = new URLSearchParams();
    if (selectedHub) query.set('hubId', selectedHub);
    if (category) query.set('category', category);
    if (startDate) query.set('startDate', startDate);
    if (endDate) query.set('endDate', endDate);
    navigate(`/vehicles?${query.toString()}`);
  };

  return (
    <div className="space-y-24 pb-20">
      
      {/* Hero Section */}
      <section className="relative pt-12 pb-20 overflow-hidden">
        {/* Glow backdrop */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[350px] bg-brand-500/10 blur-[140px] pointer-events-none rounded-full" />
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative">
          <div className="text-center max-w-3xl mx-auto space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-900 border border-slate-700 text-xs text-brand-400 font-medium">
              <span className="w-2 h-2 rounded-full bg-brand-400 animate-ping" />
              <span>Now Live Across Vijayawada Hubs</span>
            </div>

            <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white leading-tight">
              Self-Drive Vehicle Rentals <br />
              <span className="bg-gradient-to-r from-brand-400 via-emerald-300 to-teal-400 bg-clip-text text-transparent">
                Direct From Verified Partners
              </span>
            </h1>

            <p className="text-slate-400 text-base sm:text-lg max-w-2xl mx-auto leading-relaxed">
              Rent certified SUVs, sedans, and electric vehicles in Vijayawada. 
              Pay directly to local verified partners via UPI with instant 15-minute hold guarantees.
            </p>
          </div>

          {/* Search Box Card */}
          <div className="mt-10 max-w-5xl mx-auto glass-panel p-4 sm:p-6 rounded-2xl shadow-2xl border border-slate-800">
            <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              
              {/* Hub Selection */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-brand-400" />
                  <span>Pickup Hub (Vijayawada)</span>
                </label>
                <select
                  value={selectedHub}
                  onChange={(e) => setSelectedHub(e.target.value)}
                  className="w-full px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm focus:outline-none focus:border-brand-500"
                >
                  <option value="">All Vijayawada Hubs</option>
                  {hubs.map((hub) => (
                    <option key={hub.id} value={hub.id}>
                      {hub.hubName} ({hub.hubCode})
                    </option>
                  ))}
                </select>
              </div>

              {/* Category */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                  <Car className="w-3.5 h-3.5 text-brand-400" />
                  <span>Vehicle Type</span>
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm focus:outline-none focus:border-brand-500"
                >
                  <option value="">All Categories</option>
                  <option value="SUV">Compact &amp; Full SUV</option>
                  <option value="SEDAN">Executive Sedan</option>
                  <option value="HATCHBACK">City Hatchback</option>
                  <option value="EV">Electric Vehicle (EV)</option>
                  <option value="LUXURY">Premium Luxury</option>
                </select>
              </div>

              {/* Pickup Time */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-brand-400" />
                  <span>Start Date &amp; Time</span>
                </label>
                <input
                  type="datetime-local"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm focus:outline-none focus:border-brand-500"
                />
              </div>

              {/* Search Submit */}
              <div className="flex items-end">
                <button
                  type="submit"
                  className="w-full py-2.5 px-4 bg-brand-600 hover:bg-brand-500 text-slate-950 font-bold rounded-xl shadow-lg shadow-brand-500/20 transition-all flex items-center justify-center gap-2"
                >
                  <Search className="w-4 h-4" />
                  <span>Find Available Cars</span>
                </button>
              </div>

            </form>

            <div className="mt-4 pt-3 border-t border-slate-800/80 flex flex-wrap items-center justify-between text-xs text-slate-400 gap-2">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                Zero Booking Commission to Customers
              </span>
              <span className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-brand-400" />
                Instant 15-Minute Lock on Hold Creation
              </span>
              <span className="flex items-center gap-1.5">
                <Zap className="w-3.5 h-3.5 text-amber-400" />
                Direct Partner UPI Settlements
              </span>
            </div>
          </div>

        </div>
      </section>

      {/* Featured Fleet Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-brand-400">
              Verified Fleet
            </span>
            <h2 className="text-2xl sm:text-3xl font-bold text-white mt-1">
              Popular Rental Vehicles in Vijayawada
            </h2>
          </div>
          <Link
            to="/vehicles"
            className="inline-flex items-center gap-1 text-sm font-semibold text-brand-400 hover:text-brand-300"
          >
            <span>View All Fleet</span>
            <ChevronRight className="w-4 h-4" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {featuredVehicles.length > 0 ? (
            featuredVehicles.map((vehicle) => (
              <div key={vehicle.id} className="glass-card rounded-2xl overflow-hidden flex flex-col group transition-all">
                <div className="h-48 bg-slate-900 relative overflow-hidden flex items-center justify-center">
                  <img
                    src={vehicle.primaryImageUrl || 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80'}
                    alt={`${vehicle.make} ${vehicle.model}`}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute top-3 left-3 px-2.5 py-1 rounded-full bg-slate-950/80 backdrop-blur-sm border border-slate-700 text-xs font-semibold text-white">
                    {vehicle.category}
                  </div>
                  <div className="absolute top-3 right-3 px-2 py-0.5 rounded-md bg-emerald-500/20 border border-emerald-500/40 text-[11px] font-bold text-emerald-300">
                    {vehicle.fuelType} &bull; {vehicle.transmission}
                  </div>
                </div>

                <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                  <div>
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-bold text-white group-hover:text-brand-400 transition-colors">
                        {vehicle.make} {vehicle.model}
                      </h3>
                      <span className="text-xs text-slate-400 font-mono">{vehicle.year}</span>
                    </div>
                    <div className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-slate-500" />
                      <span>{vehicle.partnerHubName || 'Benz Circle Hub'}</span>
                    </div>
                  </div>

                  <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between">
                    <div>
                      <div className="text-xs text-slate-400">Daily Rental</div>
                      <div className="text-xl font-bold text-white font-mono">
                        ₹{vehicle.dailyRate ? Number(vehicle.dailyRate).toLocaleString('en-IN') : '2,200'}
                        <span className="text-xs font-normal text-slate-400">/day</span>
                      </div>
                    </div>

                    <Link
                      to={`/vehicles/${vehicle.id}`}
                      className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-brand-600 text-slate-200 hover:text-slate-950 font-semibold text-xs transition-all flex items-center gap-1.5"
                    >
                      <span>Details &amp; Book</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              </div>
            ))
          ) : (
            // Demo fallback cards if backend seeder is populating
            [
              { name: 'Hyundai Creta SX', cat: 'SUV', rate: '2,800', fuel: 'Petrol', trans: 'Auto', hub: 'Benz Circle Hub' },
              { name: 'Tata Nexon EV Max', cat: 'EV', rate: '2,500', fuel: 'Electric', trans: 'Auto', hub: 'MG Road Point' },
              { name: 'Honda City ZX', cat: 'SEDAN', rate: '2,600', fuel: 'Petrol', trans: 'Manual', hub: 'PNBS Station Hub' },
            ].map((v, i) => (
              <div key={i} className="glass-card rounded-2xl overflow-hidden p-5 space-y-4">
                <div className="h-40 bg-slate-900 rounded-xl flex items-center justify-center text-slate-600">
                  <Car className="w-12 h-12 text-slate-700" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">{v.name}</h3>
                  <p className="text-xs text-slate-400">{v.hub} &bull; {v.fuel}</p>
                </div>
                <div className="flex justify-between items-center pt-2 border-t border-slate-800">
                  <span className="text-lg font-bold font-mono text-emerald-400">₹{v.rate}/day</span>
                  <Link to="/vehicles" className="text-xs font-semibold text-brand-400 hover:underline">
                    View in Catalog &rarr;
                  </Link>
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      {/* Trust & Architecture Pillars */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="glass-panel p-8 sm:p-12 rounded-3xl border border-slate-800 space-y-10">
          <div className="text-center max-w-2xl mx-auto">
            <span className="text-xs font-semibold uppercase tracking-wider text-brand-400">
              Why Vijayawada Chooses DriveElite
            </span>
            <h2 className="text-2xl sm:text-3xl font-bold text-white mt-1">
              Built on Financial Rigor &amp; Transparent Operations
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="space-y-3 p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
              <div className="w-10 h-10 rounded-xl bg-brand-500/10 text-brand-400 flex items-center justify-center">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <h3 className="text-base font-bold text-white">Direct Partner UPI Settlement</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Platform never collects or splits rental proceeds in MVP. You remit payment straight to the local verified fleet owner’s bank account with zero escrow delays.
              </p>
            </div>

            <div className="space-y-3 p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
              <div className="w-10 h-10 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center">
                <Clock className="w-5 h-5" />
              </div>
              <h3 className="text-base font-bold text-white">Guaranteed 15-Min Reservation Hold</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Vehicle is locked exclusively for your hold window. If you transfer payment, the partner confirms your reservation with full UTR audit trace.
              </p>
            </div>

            <div className="space-y-3 p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
              <div className="w-10 h-10 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center">
                <TrendingUp className="w-5 h-5" />
              </div>
              <h3 className="text-base font-bold text-white">Late-Payment Reapplication (Safeguard A)</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Transferred funds after hold expired? Our immutable financial recovery engine allows instant 1-click reapplication to a new active hold.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Dual Business CTA */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          
          {/* Partner Box */}
          <div className="glass-card p-8 rounded-3xl border border-slate-800 flex flex-col justify-between space-y-6">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-blue-500/10 text-blue-400 flex items-center justify-center">
                <Building className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white">Are You a Vehicle Rental Agency in AP?</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                List your fleet, collect 100% of rental payments directly into your business UPI, manage return inspections with photographic handover, and scale operations.
              </p>
            </div>
            <Link
              to="/partners"
              className="inline-flex items-center justify-between px-5 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-semibold text-sm transition-colors"
            >
              <span>Partner Onboarding Guide</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {/* Contributor Box */}
          <div className="glass-card p-8 rounded-3xl border border-slate-800 flex flex-col justify-between space-y-6">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-teal-500/10 text-teal-400 flex items-center justify-center">
                <Car className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white">Own an Idle Car in Vijayawada?</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Attach your vehicle to a certified rental partner. Sign a legally-binding agreement, track mileage, receive direct monthly payables, and earn predictable passive revenue.
              </p>
            </div>
            <Link
              to="/contributors"
              className="inline-flex items-center justify-between px-5 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-semibold text-sm transition-colors"
            >
              <span>Contributor Fleet Program</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

        </div>
      </section>

    </div>
  );
};

export default HomePage;
