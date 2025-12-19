// Delivery Staff Logic Namespace
const DeliveryApp = {
    ordersUnsubscribe: null,

    init: function(staffName) {
        console.log('Initializing Delivery Dashboard for ' + staffName);
        this.loadAssignedOrders(staffName);
    },

    cleanup: function() {
        console.log('Cleaning up Delivery Dashboard');
        if (this.ordersUnsubscribe) this.ordersUnsubscribe();
    },

    loadAssignedOrders: function(staffName) {
        if (this.ordersUnsubscribe) this.ordersUnsubscribe();

        this.ordersUnsubscribe = db.collection('orders')
          .where('assignedTo', '==', staffName)
          .where('status', 'in', ['READY_FOR_DELIVERY', 'PICKED_UP', 'DELIVERED'])
          .onSnapshot(snap => {
              const container = document.getElementById('delivery-orders-list');
              if (!container) return;

              container.innerHTML = ''; // Clear previous content

              if (snap.empty) {
                  const p = document.createElement('p');
                  p.textContent = 'No assigned orders.';
                  container.appendChild(p);
                  return;
              }

              snap.forEach(doc => {
                  const order = doc.data();
                  if (order.status === 'DELIVERED') return;

                  const card = document.createElement('div');
                  card.className = `order-card status-${order.status}`;

                  // Header
                  const header = document.createElement('div');
                  header.className = 'order-header';

                  const strongId = document.createElement('strong');
                  strongId.textContent = `#${doc.id.slice(-5)}`;
                  header.appendChild(strongId);

                  const spanStatus = document.createElement('span');
                  spanStatus.textContent = order.status.replace(/_/g, ' ');
                  header.appendChild(spanStatus);

                  card.appendChild(header);

                  // Details
                  const details = document.createElement('div');
                  details.className = 'order-details';

                  const addDetail = (label, text, isLink = false) => {
                      const p = document.createElement('p');
                      const strong = document.createElement('strong');
                      strong.textContent = `${label}: `;
                      p.appendChild(strong);
                      if (isLink) {
                          const a = document.createElement('a');
                          a.href = `tel:${text}`;
                          a.textContent = text;
                          p.appendChild(a);
                      } else {
                          p.appendChild(document.createTextNode(text));
                      }
                      details.appendChild(p);
                  };

                  addDetail('Customer', order.customerName);
                  addDetail('Phone', order.customerPhoneNumber, true);
                  addDetail('Address', order.deliveryAddress);
                  addDetail('Amount', `₹${order.grandTotal.toFixed(2)}`);
                  addDetail('Payment', order.paymentMethod || 'COD');

                  card.appendChild(details);

                  // Actions
                  if (order.status === 'PICKED_UP') {
                      if (order.otpVerified) {
                          const badge = document.createElement('span');
                          badge.className = 'verified-badge';
                          badge.textContent = '✓ Delivered (OTP Verified)';
                          card.appendChild(badge);
                      } else {
                          const otpSection = document.createElement('div');
                          otpSection.className = 'otp-section';

                          const input = document.createElement('input');
                          input.type = 'text';
                          input.id = `otp-${doc.id}`;
                          input.maxLength = 4;
                          input.placeholder = 'Enter OTP';
                          input.onkeypress = (e) => DeliveryApp.handleEnter(e, doc.id);
                          otpSection.appendChild(input);

                          const btn = document.createElement('button');
                          btn.textContent = 'Verify';
                          btn.onclick = () => DeliveryApp.verifyOtp(doc.id);
                          otpSection.appendChild(btn);

                          card.appendChild(otpSection);

                          if (order.otpInvalid) {
                              const err = document.createElement('span');
                              err.className = 'error-badge';
                              err.textContent = 'Invalid OTP';
                              card.appendChild(err);
                          }
                      }
                  } else if (order.status === 'READY_FOR_DELIVERY') {
                      const btnPick = document.createElement('button');
                      btnPick.textContent = 'Mark as Picked Up';
                      btnPick.onclick = () => DeliveryApp.markPickedUp(doc.id);
                      card.appendChild(btnPick);
                  }

                  container.appendChild(card);
              });
          });
    },

    markPickedUp: function(orderId) {
        db.collection('orders').doc(orderId).update({ status: 'PICKED_UP' });
    },

    verifyOtp: function(orderId) {
        const otp = document.getElementById(`otp-${orderId}`).value;
        if (otp.length !== 4) return alert('Enter 4 digit OTP');

        db.collection('orders').doc(orderId).update({
            otpEntered: otp
        });
    },

    handleEnter: function(e, orderId) {
        if (e.key === 'Enter') this.verifyOtp(orderId);
    }
};
