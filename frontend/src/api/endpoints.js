import api from './client';

// ================= AUTH API =================
export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
  logout: () => api.post('/auth/logout'),
  refreshToken: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  getProfile: () => api.get('/auth/profile'),
  updateProfile: (data) => api.put('/auth/profile', data),
  submitKyc: (formData) => api.post('/auth/kyc', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
};

// ================= LOCATION API =================
export const locationApi = {
  getCities: () => api.get('/locations/cities'),
  getHubsByCity: (cityId) => api.get(`/locations/cities/${cityId}/hubs`),
  getAllHubs: () => api.get('/locations/hubs'),
};

// ================= VEHICLE API =================
export const vehicleApi = {
  searchVehicles: (params) => api.get('/vehicles/search', { params }),
  getVehicleDetails: (id) => api.get(`/vehicles/${id}`),
  getCategories: () => api.get('/vehicles/categories'),
};

// ================= BOOKING API =================
export const bookingApi = {
  createHold: (data) => api.post('/bookings/holds', data),
  getHold: (id) => api.get(`/bookings/holds/${id}`),
  cancelHold: (id) => api.delete(`/bookings/holds/${id}`),
  getMyBookings: () => api.get('/bookings/my'),
  getBookingDetails: (id) => api.get(`/bookings/${id}`),
  cancelBooking: (id, reason) => api.post(`/bookings/${id}/cancel`, { cancellationReason: reason }),
  requestExtension: (id, hours) => api.post(`/bookings/${id}/extend`, { requestedHours: hours }),
};

// ================= PAYMENT API =================
export const paymentApi = {
  getDemandByHold: (holdId) => api.get(`/payments/demands/hold/${holdId}`),
  getDemand: (demandId) => api.get(`/payments/demands/${demandId}`),
  submitPayment: (data) => api.post('/payments/submissions', data),
  reapplyPayment: (data) => api.post('/payments/reapply', data),
  getMyRefunds: () => api.get('/payments/refunds/my'),
  acknowledgeRefund: (refundId) => api.post(`/payments/refunds/${refundId}/acknowledge`),
  disputeRefund: (refundId, reason) => api.post(`/payments/refunds/${refundId}/dispute`, { disputeReason: reason }),
};

// ================= PARTNER API =================
export const partnerApi = {
  getProfile: () => api.get('/partners/profile'),
  getFleet: () => api.get('/partners/fleet'),
  addVehicle: (vehicleData) => api.post('/partners/fleet', vehicleData),
  updateVehicleStatus: (id, status) => api.patch(`/partners/fleet/${id}/status`, { status }),
  getPendingPayments: () => api.get('/partners/payments/pending'),
  verifyPayment: (submissionId) => api.post(`/partners/payments/submissions/${submissionId}/verify`),
  rejectPayment: (submissionId, rejectionReason) => api.post(`/partners/payments/submissions/${submissionId}/reject`, { rejectionReason }),
  recordHandover: (bookingId, handoverData) => api.post(`/partners/bookings/${bookingId}/handover`, handoverData),
  recordReturn: (bookingId, returnData) => api.post(`/partners/bookings/${bookingId}/return`, returnData),
  recordRefundDisbursement: (refundId, data) => api.post(`/partners/refunds/${refundId}/disburse`, data),
  getContributorPayables: () => api.get('/partners/payables'),
  recordPayableSettlement: (payableId, data) => api.post(`/partners/payables/${payableId}/settle`, data),
};

// ================= CONTRIBUTOR API =================
export const contributorApi = {
  getProfile: () => api.get('/contributors/profile'),
  submitVehicleApplication: (data) => api.post('/contributors/applications', data),
  getMyApplications: () => api.get('/contributors/applications'),
  getMyAgreements: () => api.get('/contributors/agreements'),
  signAgreement: (agreementId, signatureData) => api.post(`/contributors/agreements/${agreementId}/sign`, signatureData),
  getMyVehicles: () => api.get('/contributors/vehicles'),
  getMyPayables: () => api.get('/contributors/payables'),
  acknowledgeSettlement: (payableId) => api.post(`/contributors/payables/${payableId}/confirm`),
  disputeSettlement: (payableId, reason) => api.post(`/contributors/payables/${payableId}/dispute`, { disputeReason: reason }),
  requestWithdrawal: (agreementId, reason) => api.post(`/contributors/agreements/${agreementId}/withdraw`, { reason }),
};

// ================= ADMIN API =================
export const adminApi = {
  getStats: () => api.get('/admin/stats'),
  getPartnerApplications: () => api.get('/admin/partners/applications'),
  approvePartner: (id) => api.post(`/admin/partners/${id}/approve`),
  rejectPartner: (id, reason) => api.post(`/admin/partners/${id}/reject`, { reason }),
  getRefundDisputes: () => api.get('/admin/disputes/refunds'),
  resolveRefundDispute: (disputeId, data) => api.post(`/admin/disputes/refunds/${disputeId}/resolve`, data),
  getDepositDisputes: () => api.get('/admin/disputes/deposits'),
  resolveDepositDispute: (disputeId, data) => api.post(`/admin/disputes/deposits/${disputeId}/resolve`, data),
  getAuditLogs: (params) => api.get('/admin/audit-logs', { params }),
};

// ================= OPERATIONS API =================
export const operationsApi = {
  getNotifications: () => api.get('/operations/notifications'),
  markNotificationRead: (id) => api.patch(`/operations/notifications/${id}/read`),
  getSupportTickets: () => api.get('/operations/tickets'),
  createSupportTicket: (data) => api.post('/operations/tickets', data),
  getTicketMessages: (id) => api.get(`/operations/tickets/${id}/messages`),
  sendTicketMessage: (id, message) => api.post(`/operations/tickets/${id}/messages`, { message }),
  getVehicleReviews: (vehicleId) => api.get(`/operations/reviews/vehicle/${vehicleId}`),
  createReview: (bookingId, data) => api.post(`/operations/reviews/booking/${bookingId}`, data),
};
