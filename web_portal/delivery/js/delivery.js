// Delivery Staff Logic Namespace
window.DeliveryApp = {
    ordersUnsubscribe: null,
    historyUnsubscribe: null,
    staffName: null,

    init: function(staffName) {
        console.log('Initializing Delivery Dashboard for ' + staffName);
        this.staffName = staffName;
        this.staffUid = this.currentUser ? this.currentUser.uid : null;
        this.loadAssignedOrders();
        this.loadHistory();
    },

    cleanup: function() {
        console.log('Cleaning up Delivery Dashboard');
        if (this.ordersUnsubscribe) this.ordersUnsubscribe();
        if (this.historyUnsubscribe) this.historyUnsubscribe();
    },

    showSection: function(sectionId) {
        document.getElementById('active-section').style.display = 'none';
        document.getElementById('history-section').style.display = 'none';
        document.getElementById('nav-active').classList.remove('active');
        document.getElementById('nav-history').classList.remove('active');

        document.getElementById(sectionId + '-section').style.display = 'block';
        document.getElementById('nav-' + sectionId).classList.add('active');
    },

    loadAssignedOrders: function() {
        if (this.ordersUnsubscribe) this.ordersUnsubscribe();

        const uid = this.currentUser ? this.currentUser.uid : auth.currentUser.uid;

        // Active: READY_FOR_DELIVERY or PICKED_UP
        this.ordersUnsubscribe = db.collection('orders')
          .where('assignedToUid', '==', uid)
          .where('status', 'in', ['READY_FOR_DELIVERY', 'PICKED_UP'])
          .onSnapshot(snap => {
              const container = document.getElementById('delivery-orders-list');
              if (!container) return;

              container.innerHTML = '';

              if (snap.empty) {
                  const p = document.createElement('p');
                  p.textContent = 'No currently active assigned orders.';
                  container.appendChild(p);
                  return;
              }

              // Group orders by status
              const ordersByStatus = {
                  'READY_FOR_DELIVERY': [],
                  'PICKED_UP': []
              };

              snap.forEach(doc => {
                  const order = doc.data();
                  if (ordersByStatus[order.status]) {
                      ordersByStatus[order.status].push({ id: doc.id, data: order });
                  }
              });

              // Create Accordion for each status
              const statusOrder = ['READY_FOR_DELIVERY', 'PICKED_UP'];

              statusOrder.forEach(status => {
                  const orders = ordersByStatus[status];
                  const count = orders.length;

                  // Status Block
                  const block = document.createElement('div');
                  block.className = 'status-block';
                  block.style.marginBottom = '20px';
                  block.style.background = 'white';
                  block.style.borderRadius = '8px';
                  block.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1)';
                  block.style.overflow = 'hidden';

                  // Header
                  const header = document.createElement('div');
                  header.className = 'status-header';
                  header.style.padding = '15px';
                  header.style.background = '#f8f8f8';
                  header.style.cursor = 'pointer';
                  header.style.display = 'flex';
                  header.style.justifyContent = 'space-between';
                  header.style.alignItems = 'center';
                  header.style.borderBottom = '1px solid #eee';

                  const title = document.createElement('h3');
                  title.style.margin = '0';
                  title.style.color = '#333';
                  title.textContent = `${status.replace(/_/g, ' ')} (${count})`;

                  const toggleIcon = document.createElement('span');
                  toggleIcon.textContent = '▶'; // Default collapsed

                  header.appendChild(title);
                  header.appendChild(toggleIcon);

                  // Content
                  const content = document.createElement('div');
                  content.className = 'status-content';
                  content.style.display = 'none'; // Default collapsed
                  content.style.padding = '10px';

                  if (count === 0) {
                      content.innerHTML = '<p style="padding: 5px; color: #777;">No orders in this status.</p>';
                  } else {
                      orders.forEach(item => {
                          const card = this.createOrderCard(item.id, item.data, false);
                          content.appendChild(card);
                      });
                  }

                  // Toggle Logic
                  header.onclick = () => {
                      const isHidden = content.style.display === 'none';
                      content.style.display = isHidden ? 'block' : 'none';
                      toggleIcon.textContent = isHidden ? '▼' : '▶';
                  };

                  block.appendChild(header);
                  block.appendChild(content);
                  container.appendChild(block);
              });
          });
    },

    loadHistory: function() {
        if (this.historyUnsubscribe) this.historyUnsubscribe();

        const uid = this.currentUser ? this.currentUser.uid : auth.currentUser.uid;

        // History: DELIVERED
        this.historyUnsubscribe = db.collection('orders')
            .where('assignedToUid', '==', uid)
            .where('status', '==', 'DELIVERED')
            .limit(20)
            .onSnapshot(snap => {
                const container = document.getElementById('delivery-history-list');
                if (!container) return;

                container.innerHTML = '';

                if (snap.empty) {
                    container.innerHTML = '<p>No delivery history found.</p>';
                    return;
                }

                snap.forEach(doc => {
                    const order = doc.data();
                    const card = this.createOrderCard(doc.id, order, true);
                    container.appendChild(card);
                });
            });
    },

    createOrderCard: function(docId, order, isHistory) {
        const card = document.createElement('div');
        card.className = `order-card status-${order.status}`;

        // Header
        const header = document.createElement('div');
        header.className = 'order-header';

        const strongId = document.createElement('strong');
        strongId.textContent = `#${docId.slice(-5)}`;
        header.appendChild(strongId);

        const spanStatus = document.createElement('span');
        spanStatus.textContent = order.status.replace(/_/g, ' ');
        header.appendChild(spanStatus);

        const expandIcon = document.createElement('span');
        expandIcon.textContent = ' ▼';
        expandIcon.style.float = 'right';
        header.appendChild(expandIcon);

        // Make header clickable to toggle details
        header.style.cursor = 'pointer';
        header.onclick = () => {
             const isHidden = details.style.display === 'none';
             details.style.display = isHidden ? 'block' : 'none';
             expandIcon.textContent = isHidden ? ' ▲' : ' ▼';
        };

        card.appendChild(header);

        // Details
        const details = document.createElement('div');
        details.className = 'order-details';
        details.style.display = 'none'; // Collapsed by default

        const addDetail = (label, text, type = 'text') => {
            const p = document.createElement('p');
            const strong = document.createElement('strong');
            strong.textContent = `${label}: `;
            p.appendChild(strong);

            if (type === 'phone') {
                const a = document.createElement('a');
                a.href = `tel:${text}`;
                a.textContent = text;
                p.appendChild(a);
            } else if (type === 'address') {
                const a = document.createElement('a');
                a.href = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(text)}`;
                a.target = '_blank';
                a.textContent = text;
                a.style.color = '#1976D2';
                a.style.textDecoration = 'underline';
                p.appendChild(a);
            } else {
                p.appendChild(document.createTextNode(text));
            }
            details.appendChild(p);
        };

        const dateStr = new Date(order.orderDate).toLocaleString();
        addDetail('Date', dateStr);
        addDetail('Customer', order.customerName);
        addDetail('Phone', order.customerPhoneNumber, 'phone');
        addDetail('Address', order.deliveryAddress, 'address');
        addDetail('Payment', order.paymentMethod || 'COD');

        // Detailed Items
        const itemsDiv = document.createElement('div');
        itemsDiv.style.marginTop = '10px';
        itemsDiv.style.padding = '10px';
        itemsDiv.style.background = '#f9f9f9';
        itemsDiv.style.borderRadius = '4px';

        const itemsTitle = document.createElement('h4');
        itemsTitle.textContent = 'Order Items';
        itemsTitle.style.margin = '0 0 5px 0';
        itemsTitle.style.fontSize = '1em';
        itemsDiv.appendChild(itemsTitle);

        const ul = document.createElement('ul');
        ul.style.paddingLeft = '20px';
        ul.style.margin = '0';

        // Check for 'items' (new structure) or fallback to 'cartItems' (old structure)
        const items = (order.items && Array.isArray(order.items)) ? order.items :
                     (order.cartItems && Array.isArray(order.cartItems)) ? order.cartItems : [];

        if (items.length > 0) {
            items.forEach(item => {
                const li = document.createElement('li');
                let text = `${item.name} x ${item.quantity}`;
                if (item.notes) text += ` (${item.notes})`;
                li.textContent = text;
                ul.appendChild(li);
            });
        }
        itemsDiv.appendChild(ul);

        // Breakdown
        const breakdown = document.createElement('div');
        breakdown.style.marginTop = '10px';
        breakdown.style.borderTop = '1px solid #ddd';
        breakdown.style.paddingTop = '5px';

        const sub = order.subtotal !== undefined ? order.subtotal.toFixed(2) : '0.00';
        const fee = order.deliveryFee !== undefined ? order.deliveryFee.toFixed(2) : '0.00';
        const dist = order.deliveryDistanceKm !== undefined ? order.deliveryDistanceKm.toFixed(2) : '--';
        const total = order.grandTotal !== undefined ? order.grandTotal.toFixed(2) : '0.00';

        breakdown.innerHTML = `
            <div style="display:flex; justify-content:space-between;"><span>Subtotal:</span><span>₹${sub}</span></div>
            <div style="display:flex; justify-content:space-between;"><span>Distance:</span><span>${dist} km</span></div>
            <div style="display:flex; justify-content:space-between;"><span>Del. Fee:</span><span>₹${fee}</span></div>
            <div style="display:flex; justify-content:space-between; font-weight:bold; margin-top:5px;"><span>Total:</span><span>₹${total}</span></div>
        `;
        itemsDiv.appendChild(breakdown);
        details.appendChild(itemsDiv);

        card.appendChild(details);

        // Actions (Only for active)
        if (!isHistory) {
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
                    input.id = `otp-${docId}`;
                    input.maxLength = 4;
                    input.placeholder = 'Enter OTP';
                    input.onkeypress = (e) => DeliveryApp.handleEnter(e, docId);
                    otpSection.appendChild(input);

                    const btn = document.createElement('button');
                    btn.textContent = 'Verify';
                    btn.onclick = () => DeliveryApp.verifyOtp(docId);
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
                btnPick.onclick = () => DeliveryApp.markPickedUp(docId);
                card.appendChild(btnPick);
            }
        }

        return card;
    },

    markPickedUp: function(orderId) {
        db.collection('orders').doc(orderId).update({ status: 'PICKED_UP' });
    },

    verifyOtp: function(orderId) {
        const otp = document.getElementById(`otp-${orderId}`).value;
        if (otp.length !== 4) return alert('Enter 4 digit OTP');

        db.collection('orders').doc(orderId).update({
            otpEntered: otp
        }).then(() => {
            alert('OTP submitted. Verification pending...');
        }).catch(err => {
            alert('Error submitting OTP: ' + err.message);
        });
    },

    handleEnter: function(e, orderId) {
        if (e.key === 'Enter') this.verifyOtp(orderId);
    }
};
