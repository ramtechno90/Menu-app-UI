// Delivery Staff App

let ordersUnsubscribe = null;

auth.onAuthStateChanged(user => {
    if (user) {
        // Get Staff Name
        db.collection('delivery_staff').doc(user.uid).get().then(doc => {
            if (doc.exists) {
                const staffData = doc.data();
                const staffName = staffData.name;
                document.getElementById('auth-container').style.display = 'none';
                document.getElementById('app-container').style.display = 'block';
                loadAssignedOrders(staffName);
            } else {
                alert('Not a registered delivery staff.');
                auth.signOut();
            }
        });
    } else {
        if (ordersUnsubscribe) ordersUnsubscribe();
        document.getElementById('auth-container').style.display = 'block';
        document.getElementById('app-container').style.display = 'none';
    }
});

function login() {
    const email = document.getElementById('email').value;
    const pass = document.getElementById('password').value;
    auth.signInWithEmailAndPassword(email, pass).catch(e => alert(e.message));
}

function logout() {
    auth.signOut();
}

function loadAssignedOrders(staffName) {
    if (ordersUnsubscribe) ordersUnsubscribe();

    // Listen for orders assigned to this staff
    ordersUnsubscribe = db.collection('orders')
      .where('assignedTo', '==', staffName)
      .where('status', 'in', ['READY_FOR_DELIVERY', 'PICKED_UP', 'DELIVERED'])
      .onSnapshot(snap => {
          const container = document.getElementById('orders-list');
          container.innerHTML = '';

          if (snap.empty) {
              container.innerHTML = '<p>No assigned orders.</p>';
              return;
          }

          snap.forEach(doc => {
              const order = doc.data();
              if (order.status === 'DELIVERED') return; // Double check

              const card = document.createElement('div');
              card.className = `order-card status-${order.status}`;

              let otpContent = '';
              if (order.status === 'PICKED_UP') {
                  if (order.otpVerified) {
                      otpContent = `<span class="verified-badge">✓ Delivered (OTP Verified)</span>`;
                  } else {
                      otpContent = `
                          <div class="otp-section">
                              <input type="text" id="otp-${doc.id}" maxlength="4" placeholder="Enter OTP" onkeypress="handleEnter(event, '${doc.id}')">
                              <button onclick="verifyOtp('${doc.id}')">Verify</button>
                          </div>
                          ${order.otpInvalid ? '<span class="error-badge">Invalid OTP</span>' : ''}
                      `;
                  }
              } else if (order.status === 'READY_FOR_DELIVERY') {
                  otpContent = `<button onclick="markPickedUp('${doc.id}')">Mark as Picked Up</button>`;
              }

              card.innerHTML = `
                  <div class="order-header">
                      <strong>#${doc.id.slice(-5)}</strong>
                      <span>${order.status.replace(/_/g, ' ')}</span>
                  </div>
                  <div class="order-details">
                      <p><strong>Customer:</strong> ${order.customerName}</p>
                      <p><strong>Phone:</strong> <a href="tel:${order.customerPhoneNumber}">${order.customerPhoneNumber}</a></p>
                      <p><strong>Address:</strong> ${order.deliveryAddress}</p>
                      <p><strong>Amount:</strong> ₹${order.grandTotal.toFixed(2)}</p>
                      <p><strong>Payment:</strong> ${order.paymentMethod || 'COD'}</p>
                  </div>
                  ${otpContent}
              `;
              container.appendChild(card);
          });
      });
}

function markPickedUp(orderId) {
    db.collection('orders').doc(orderId).update({ status: 'PICKED_UP' });
}

function verifyOtp(orderId) {
    const otp = document.getElementById(`otp-${orderId}`).value;
    if (otp.length !== 4) return alert('Enter 4 digit OTP');

    // Update otpEntered to trigger Cloud Function
    db.collection('orders').doc(orderId).update({
        otpEntered: otp
    });
}

function handleEnter(e, orderId) {
    if (e.key === 'Enter') verifyOtp(orderId);
}
