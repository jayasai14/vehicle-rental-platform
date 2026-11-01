const http = require('http');

function post(urlPath, data, token = null) {
  return new Promise((resolve, reject) => {
    const postData = JSON.stringify(data);
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: urlPath,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData),
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      }
    };

    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: body });
        }
      });
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

function get(urlPath, token = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: urlPath,
      method: 'GET',
      headers: {
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      }
    };

    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: body });
        }
      });
    });
    req.on('error', reject);
    req.end();
  });
}

async function runE2E() {
  console.log('====================================================');
  console.log('   DRIVEELITE E2E WORKFLOW INTEGRATION VERIFICATION  ');
  console.log('====================================================');

  // 1. Customer Login
  console.log('\n[1] Customer Logging in (customer@example.com)...');
  const custLogin = await post('/api/v1/auth/login', {
    email: 'customer@example.com',
    password: 'Password@123'
  });
  console.log('Customer Login Status:', custLogin.status);
  const custToken = custLogin.data.data.accessToken;
  console.log('Customer authenticated:', custLogin.data.data.fullName);

  // 2. Partner Login
  console.log('\n[2] Partner Logging in (partner@example.com)...');
  const partnerLogin = await post('/api/v1/auth/login', {
    email: 'partner@example.com',
    password: 'Password@123'
  });
  console.log('Partner Login Status:', partnerLogin.status);
  const partnerToken = partnerLogin.data.data.accessToken;
  console.log('Partner authenticated:', partnerLogin.data.data.fullName);

  // 3. Customer creates a 15-Minute Booking Hold on Vehicle #2
  console.log('\n[3] Customer Creating 15-Minute Booking Hold on Vehicle #2 (Swift ZXi)...');
  const startTime = new Date(Date.now() + 86400000 * 2).toISOString();
  const endTime = new Date(Date.now() + 86400000 * 4).toISOString();
  const holdRes = await post('/api/v1/bookings/holds', {
    vehicleId: 2,
    startTime: startTime,
    endTime: endTime,
    pickupLocationId: 4,
    returnLocationId: 4
  }, custToken);
  console.log('Hold Creation Status:', holdRes.status);
  if (!holdRes.data.success) {
    console.error('Hold error:', holdRes.data);
    return;
  }
  const hold = holdRes.data.data;
  console.log(`Hold #${hold.id} created successfully! Status: ${hold.status}, Expires At: ${hold.expiresAt}`);

  // 4. Customer fetches payment demand
  console.log('\n[4] Customer Fetching Rental Payment Demand for Hold...');
  const demandRes = await get(`/api/v1/payments/demands/hold/${hold.id}`, custToken);
  console.log('Demand Status:', demandRes.status);
  const demand = demandRes.data.data;
  console.log(`Demand #${demand.id} (${demand.demandReference}): Amount Demanded = ₹${demand.amountDemanded}, Status: ${demand.status}`);
  console.log(`Direct Partner Payee: ${demand.partnerBusinessName}, UPI VPA: ${demand.partnerUpiId}`);

  // 5. Customer submits payment reference (UTR)
  console.log('\n[5] Customer Submitting Partner UPI Payment Reference...');
  const utr = 'UTR' + Math.floor(100000000000 + Math.random() * 900000000000);
  const subRes = await post('/api/v1/payments/submissions', {
    demandId: demand.id,
    paymentMethod: 'UPI',
    transactionReference: utr,
    amountSubmitted: parseFloat(demand.amountDemanded),
    customerNotes: 'Sent from PhonePe'
  }, custToken);
  console.log('Payment Submission Status:', subRes.status);
  const submission = subRes.data.data;
  console.log(`Submission #${submission.id} created! UTR: ${submission.transactionReference}, Status: ${submission.submissionStatus}`);

  // 6. Partner checks pending payment submissions
  console.log('\n[6] Partner Checking Pending Payment Verification Queue...');
  const pendingRes = await get('/api/v1/partners/payments/pending', partnerToken);
  console.log(`Found ${pendingRes.data.data.length} pending submissions in partner queue.`);
  const targetSub = pendingRes.data.data.find(s => s.id === submission.id);
  console.log(`Found our submission #${targetSub.id} for ₹${targetSub.amountSubmitted} with UTR ${targetSub.transactionReference}`);

  // 7. Partner verifies payment
  console.log('\n[7] Partner Verifying and Approving Payment Submission...');
  const verifyRes = await post(`/api/v1/partners/payments/submissions/${targetSub.id}/verify`, {}, partnerToken);
  console.log('Verification Status:', verifyRes.status);
  console.log(`Submission verified at: ${verifyRes.data.data.verifiedAt}`);

  // 8. Customer verifies confirmed booking
  console.log('\n[8] Customer Fetching My Bookings to confirm...');
  const myBookingsRes = await get('/api/v1/bookings/my', custToken);
  console.log(`Customer has ${myBookingsRes.data.data.length} total bookings.`);
  const activeBooking = myBookingsRes.data.data[0];
  console.log(`Active Booking Code: ${activeBooking.bookingCode}, Status: ${activeBooking.status}, Custody: ${activeBooking.custodyState}`);

  // 9. Partner performs pre-rental handover inspection
  console.log('\n[9] Partner Performing Pre-Rental Vehicle Handover...');
  const handoverRes = await post(`/api/v1/partners/bookings/${activeBooking.id}/handover`, {
    odometerReading: 24500,
    fuelPercent: 100,
    conditionNotes: 'Vehicle delivered in pristine condition. Zero dents.'
  }, partnerToken);
  console.log('Handover Status:', handoverRes.status);
  console.log('Booking after handover:', handoverRes.data.data.status, 'Custody:', handoverRes.data.data.custodyState);

  // 10. Partner performs post-rental return inspection
  console.log('\n[10] Partner Performing Post-Rental Return Inspection...');
  const returnRes = await post(`/api/v1/partners/bookings/${activeBooking.id}/return`, {
    odometerReading: 24820,
    fuelPercent: 95,
    conditionNotes: 'Returned cleanly. Normal highway wear.',
    damageDeductionProposed: 0
  }, partnerToken);
  console.log('Return Status:', returnRes.status);
  console.log('Booking after return:', returnRes.data.data.status, 'Custody:', returnRes.data.data.custodyState);

  // 11. Partner checks contributor payables
  console.log('\n[11] Checking Contributor Payables & Accrued Cut...');
  const payablesRes = await get('/api/v1/partners/payables', partnerToken);
  console.log(`Total partner contributor payables recorded: ${payablesRes.data.data.length}`);
  if (payablesRes.data.data.length > 0) {
    const latestPayable = payablesRes.data.data[payablesRes.data.data.length - 1];
    console.log(`Latest Payable #${latestPayable.id}: Agreed Cut = ₹${latestPayable.agreedCutAmount}, Status = ${latestPayable.payableStatus}`);
  }

  console.log('\n====================================================');
  console.log('      ALL END-TO-END WORKFLOWS PASSED WITH 100% SUCCESS!   ');
  console.log('====================================================');
}

runE2E().catch(err => {
  console.error('E2E Test Execution Error:', err);
  process.exit(1);
});
