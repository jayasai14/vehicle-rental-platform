import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { 
  Car, 
  MapPin, 
  Filter, 
  Fuel, 
  Gauge, 
  CheckCircle, 
  ArrowRight,
  SlidersHorizontal,
  RotateCcw
} from 'lucide-react';
import { vehicleApi, locationApi } from '../api/endpoints';

export const VehiclesPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [vehicles, setVehicles] = useState([]);
  const [hubs, setHubs] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filters from query params
  const hubId = searchParams.get('hubId') || '';
  const category = searchParams.get('category') || '';
  const fuelType = searchParams.get('fuelType') || '';
  const transmission = searchParams.get('transmission') || '';
  const sortBy = searchParams.get('sortBy') || 'dailyRate';
  const sortDirection = searchParams.get('sortDirection') || 'ASC';

  useEffect(() => {
    locationApi.getAllHubs()
      .then(res => setHubs(res.data?.data || []))
      .catch(err => console.error(err));
  }, []);

  useEffect(() => {
    setLoading(true);
    const params = {
      hubId: hubId || undefined,
      category: category || undefined,
      fuelType: fuelType || undefined,
      transmission: transmission || undefined,
      sortBy: sortBy,
      sortDirection: sortDirection,
      page: 0,
      size: 24,
    };

    vehicleApi.searchVehicles(params)
      .then(res => {
        setVehicles(res.data?.data?.content || []);
      })
      .catch(err => {
        console.error('Error fetching vehicles:', err);
      })
      .finally(() => setLoading(false));
  }, [hubId, category, fuelType, transmission, sortBy, sortDirection]);

  const updateFilter = (key, value) => {
    const next = new URLSearchParams(searchParams);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    setSearchParams(next);
  };

  const clearFilters = () => {
    setSearchParams(new URLSearchParams());
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-brand-400">
            Vijayawada Fleet Catalogue
          </span>
          <h1 className="text-3xl font-extrabold text-white mt-1">Available Rental Vehicles</h1>
          <p className="text-xs text-slate-400 mt-1">
            Choose a verified vehicle with instant 15-minute booking hold and direct partner UPI settlement.
          </p>
        </div>

        {/* Clear Filters */}
        {(hubId || category || fuelType || transmission) && (
          <button
            onClick={clearFilters}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700 text-xs text-slate-300 hover:text-white hover:bg-slate-800 transition-colors self-start sm:self-auto"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset Filters</span>
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        
        {/* Sidebar Filters */}
        <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-6 h-fit lg:sticky lg:top-28">
          <div className="flex items-center gap-2 text-sm font-bold text-white border-b border-slate-800 pb-3">
            <SlidersHorizontal className="w-4 h-4 text-brand-400" />
            <span>Filter Catalogue</span>
          </div>

          {/* Hub Filter */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300">Pickup Hub</label>
            <select
              value={hubId}
              onChange={(e) => updateFilter('hubId', e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
            >
              <option value="">All Vijayawada Hubs</option>
              {hubs.map((hub) => (
                <option key={hub.id} value={hub.id}>
                  {hub.hubName}
                </option>
              ))}
            </select>
          </div>

          {/* Category Filter */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300">Vehicle Category</label>
            <div className="grid grid-cols-2 gap-2 text-xs">
              {['SUV', 'SEDAN', 'HATCHBACK', 'EV', 'LUXURY'].map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => updateFilter('category', category === cat ? '' : cat)}
                  className={`px-2.5 py-1.5 rounded-lg border text-center transition-all ${
                    category === cat
                      ? 'bg-brand-500/20 border-brand-500 text-brand-300 font-bold'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          {/* Fuel Filter */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300">Fuel Type</label>
            <select
              value={fuelType}
              onChange={(e) => updateFilter('fuelType', e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
            >
              <option value="">All Fuel Types</option>
              <option value="PETROL">Petrol</option>
              <option value="DIESEL">Diesel</option>
              <option value="ELECTRIC">Electric (EV)</option>
            </select>
          </div>

          {/* Transmission */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300">Transmission</label>
            <div className="grid grid-cols-2 gap-2 text-xs">
              {['AUTOMATIC', 'MANUAL'].map((trans) => (
                <button
                  key={trans}
                  type="button"
                  onClick={() => updateFilter('transmission', transmission === trans ? '' : trans)}
                  className={`px-2.5 py-1.5 rounded-lg border text-center transition-all ${
                    transmission === trans
                      ? 'bg-brand-500/20 border-brand-500 text-brand-300 font-bold'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  {trans === 'AUTOMATIC' ? 'Automatic' : 'Manual'}
                </button>
              ))}
            </div>
          </div>

          {/* Sort Order */}
          <div className="space-y-2 pt-2 border-t border-slate-800">
            <label className="text-xs font-semibold text-slate-300">Sort By</label>
            <select
              value={`${sortBy}-${sortDirection}`}
              onChange={(e) => {
                const [sort, dir] = e.target.value.split('-');
                const next = new URLSearchParams(searchParams);
                next.set('sortBy', sort);
                next.set('sortDirection', dir);
                setSearchParams(next);
              }}
              className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-white text-xs focus:outline-none focus:border-brand-500"
            >
              <option value="dailyRate-ASC">Price: Low to High</option>
              <option value="dailyRate-DESC">Price: High to Low</option>
              <option value="year-DESC">Year: Newest First</option>
            </select>
          </div>

        </div>

        {/* Vehicles Grid */}
        <div className="lg:col-span-3 space-y-6">
          {loading ? (
            <div className="min-h-[40vh] flex items-center justify-center">
              <div className="w-10 h-10 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : vehicles.length === 0 ? (
            <div className="glass-card p-12 rounded-2xl text-center space-y-4">
              <Car className="w-16 h-16 text-slate-600 mx-auto" />
              <h3 className="text-xl font-bold text-white">No vehicles found</h3>
              <p className="text-xs text-slate-400 max-w-sm mx-auto">
                No active rental vehicles match your filter criteria in Vijayawada right now. Try adjusting your filters or resetting them.
              </p>
              <button
                onClick={clearFilters}
                className="px-4 py-2 rounded-xl bg-brand-600 text-slate-950 font-bold text-xs"
              >
                Reset All Filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {vehicles.map((vehicle) => (
                <div key={vehicle.id} className="glass-card rounded-2xl overflow-hidden flex flex-col justify-between group hover:border-brand-500/40 transition-all">
                  
                  {/* Image container */}
                  <div className="h-44 bg-slate-900 relative overflow-hidden flex items-center justify-center">
                    <img
                      src={vehicle.primaryImageUrl || 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80'}
                      alt={`${vehicle.make} ${vehicle.model}`}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                    <div className="absolute top-3 left-3 px-2 py-0.5 rounded-full bg-slate-950/80 backdrop-blur-sm border border-slate-700 text-[11px] font-semibold text-white">
                      {vehicle.category}
                    </div>
                    <div className="absolute top-3 right-3 px-2 py-0.5 rounded-md bg-emerald-500/20 border border-emerald-500/40 text-[10px] font-bold text-emerald-300">
                      {vehicle.fuelType}
                    </div>
                  </div>

                  {/* Body Content */}
                  <div className="p-4 flex-1 flex flex-col justify-between space-y-4">
                    <div>
                      <div className="flex items-center justify-between">
                        <h3 className="text-base font-bold text-white group-hover:text-brand-400 transition-colors">
                          {vehicle.make} {vehicle.model}
                        </h3>
                        <span className="text-xs text-slate-400 font-mono">{vehicle.year}</span>
                      </div>
                      
                      <div className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                        <span className="truncate">{vehicle.partnerHubName || 'Benz Circle Hub'}</span>
                      </div>

                      {/* Specs */}
                      <div className="grid grid-cols-2 gap-2 mt-3 text-[11px] text-slate-300 bg-slate-950/60 p-2 rounded-lg border border-slate-800">
                        <div className="flex items-center gap-1">
                          <Gauge className="w-3 h-3 text-brand-400" />
                          <span>{vehicle.transmission}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Fuel className="w-3 h-3 text-teal-400" />
                          <span>{vehicle.fuelType}</span>
                        </div>
                      </div>
                    </div>

                    {/* Pricing & CTA */}
                    <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between">
                      <div>
                        <div className="text-[10px] uppercase text-slate-400 font-medium">Daily Rental</div>
                        <div className="text-lg font-bold text-white font-mono">
                          ₹{vehicle.dailyRate ? Number(vehicle.dailyRate).toLocaleString('en-IN') : '2,200'}
                          <span className="text-[10px] font-normal text-slate-400">/day</span>
                        </div>
                        <div className="text-[10px] text-slate-400">
                          Deposit: ₹{vehicle.securityDepositAmount ? Number(vehicle.securityDepositAmount).toLocaleString('en-IN') : '5,000'}
                        </div>
                      </div>

                      <Link
                        to={`/vehicles/${vehicle.id}`}
                        className="px-3.5 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-slate-950 font-bold text-xs shadow-md shadow-brand-500/10 transition-all flex items-center gap-1"
                      >
                        <span>Reserve</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Link>
                    </div>

                  </div>

                </div>
              ))}
            </div>
          )}
        </div>

      </div>

    </div>
  );
};

export default VehiclesPage;
