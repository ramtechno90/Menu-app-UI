// Admin Logic Namespace
window.AdminApp = {
    categories: [],
    menuItems: [],
    staffList: [],
    menuItemsUnsubscribe: null,
    categoriesUnsubscribe: null,
    ordersUnsubscribe: null,
    historyUnsubscribe: null,

    init: function() {
        console.log('Initializing Admin Dashboard');
        this.registerServiceWorker();
        this.requestNotificationPermission();
        this.loadMenuManagement();
        this.loadDeliveryStaff().then(() => {
            this.loadOrders();
            this.loadHistory();
        });
    },

    registerServiceWorker: function() {
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('sw.js')
                .then(reg => console.log('Service Worker registered', reg))
                .catch(err => console.log('Service Worker registration failed', err));
        }
    },

    requestNotificationPermission: function() {
        if ('Notification' in window && Notification.permission !== 'granted') {
            Notification.requestPermission();
        }
    },

    cleanup: function() {
        console.log('Cleaning up Admin Dashboard');
        if (this.ordersUnsubscribe) this.ordersUnsubscribe();
        if (this.historyUnsubscribe) this.historyUnsubscribe();
        if (this.menuItemsUnsubscribe) this.menuItemsUnsubscribe();
        if (this.categoriesUnsubscribe) this.categoriesUnsubscribe();
    },

    loadDeliveryStaff: function() {
        return db.collection('delivery_staff').get().then(snap => {
            this.staffList = [];
            snap.forEach(doc => {
                const data = doc.data();
                if (data.name) {
                    this.staffList.push(data.name);
                }
            });
        }).catch(err => {
            console.error("Error loading staff:", err);
        });
    },

    // --- ORDERS ---
    loadOrders: function() {
        if (this.ordersUnsubscribe) this.ordersUnsubscribe();

        let isFirstLoad = true;

        this.ordersUnsubscribe = db.collection('orders')
            .where('status', 'in', ['PENDING', 'PREPARING', 'READY_FOR_DELIVERY', 'PICKED_UP'])
            .orderBy('orderDate', 'desc')
            .onSnapshot(snapshot => {
            const list = document.getElementById('orders-list');
            if (!list) return;

            // Handle Notifications for new orders
            if (!isFirstLoad) {
                snapshot.docChanges().forEach(change => {
                    if (change.type === 'added') {
                        const order = change.doc.data();
                        if (order.status === 'PENDING') {
                            this.sendNotification(change.doc.id, order);
                        }
                    }
                });
            }
            isFirstLoad = false;

            list.innerHTML = '';
            if (snapshot.empty) {
                list.innerHTML = '<p>No active orders.</p>';
                return;
            }

            const table = document.createElement('table');
            table.innerHTML = '<thead><tr><th>Date</th><th>Customer</th><th>Details</th><th>Status</th><th>Assigned To</th><th>Actions</th></tr></thead>';
            const tbody = document.createElement('tbody');

            snapshot.forEach(doc => {
                const order = doc.data();
                const tr = this.createOrderRow(doc.id, order, false);
                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            list.appendChild(table);
        });
    },

    sendNotification: function(orderId, order) {
        if (!('Notification' in window)) return;

        if (Notification.permission === 'granted') {
            const title = 'New Order Received!';
            const options = {
                body: `Order #${orderId.slice(-5)} from ${order.customerName}\nTotal: ₹${order.grandTotal}`,
                icon: 'https://via.placeholder.com/128?text=Pizza',
                vibrate: [200, 100, 200],
                requireInteraction: true,
                tag: 'new-order',
                renotify: true
            };

            if ('serviceWorker' in navigator) {
                navigator.serviceWorker.ready.then(registration => {
                    registration.showNotification(title, options);
                });
            } else {
                const notif = new Notification(title, options);
                notif.onclick = () => {
                    window.focus();
                    notif.close();
                };
            }
        }
    },

    loadHistory: function() {
        if (this.historyUnsubscribe) this.historyUnsubscribe();

        this.historyUnsubscribe = db.collection('orders')
            .where('status', 'in', ['DELIVERED', 'REJECTED'])
            .orderBy('orderDate', 'desc')
            .limit(50)
            .onSnapshot(snapshot => {
            const list = document.getElementById('history-list');
            if (!list) return;

            list.innerHTML = '';
            if (snapshot.empty) {
                list.innerHTML = '<p>No delivered orders found.</p>';
                return;
            }

            const table = document.createElement('table');
            table.innerHTML = '<thead><tr><th>Date</th><th>Customer</th><th>Details</th><th>Status</th><th>Assigned To</th><th>Actions</th></tr></thead>';
            const tbody = document.createElement('tbody');

            snapshot.forEach(doc => {
                const order = doc.data();
                const tr = this.createOrderRow(doc.id, order, true);
                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            list.appendChild(table);
        });
    },

    createOrderRow: function(docId, order, isHistory) {
        const tr = document.createElement('tr');
        tr.onclick = (e) => {
            if (['BUTTON', 'SELECT', 'INPUT', 'OPTION'].includes(e.target.tagName)) return;
            tr.classList.toggle('expanded');
        };
        tr.classList.add('expandable-row');

        const date = new Date(order.orderDate).toLocaleString();

        const tdDate = document.createElement('td');
        tdDate.setAttribute('data-label', 'Date');
        tdDate.textContent = date;
        tr.appendChild(tdDate);

        const tdCustomer = document.createElement('td');
        tdCustomer.setAttribute('data-label', 'Customer');
        const divCust = document.createElement('div');
        divCust.textContent = order.customerName;
        const smallAddr = document.createElement('small');
        smallAddr.textContent = order.deliveryAddress;
        smallAddr.style.display = 'block';
        tdCustomer.appendChild(divCust);
        tdCustomer.appendChild(smallAddr);
        tr.appendChild(tdCustomer);

        const tdDetails = document.createElement('td');
        tdDetails.setAttribute('data-label', 'Details');

        const itemList = document.createElement('ul');
        itemList.style.paddingLeft = '20px';
        itemList.style.margin = '0 0 10px 0';
        itemList.style.fontSize = '0.9em';

        const items = (order.items && Array.isArray(order.items)) ? order.items :
                     (order.cartItems && Array.isArray(order.cartItems)) ? order.cartItems : [];

        if (items.length > 0) {
            items.forEach(item => {
                const li = document.createElement('li');
                let text = `${item.name} (x${item.quantity})`;
                if (item.notes) {
                    text += ` - Note: ${item.notes}`;
                }
                if (item.price) {
                     text += ` - ₹${(item.price * item.quantity).toFixed(2)}`;
                }
                li.textContent = text;
                itemList.appendChild(li);
            });
        }
        tdDetails.appendChild(itemList);

        const breakdown = document.createElement('div');
        breakdown.style.fontSize = '0.85em';
        breakdown.style.color = '#555';

        const sub = order.subtotal !== undefined ? order.subtotal.toFixed(2) : '0.00';
        const tax = order.tax !== undefined ? order.tax.toFixed(2) : '0.00';
        const fee = order.deliveryFee !== undefined ? order.deliveryFee.toFixed(2) : '0.00';
        const total = order.grandTotal !== undefined ? order.grandTotal.toFixed(2) : '0.00';

        breakdown.innerHTML = `
            <div style="display:flex; justify-content:space-between; border-top:1px solid #eee; padding-top:4px;"><span>Subtotal:</span><span>₹${sub}</span></div>
            <div style="display:flex; justify-content:space-between;"><span>Tax:</span><span>₹${tax}</span></div>
            <div style="display:flex; justify-content:space-between;"><span>Delivery Fee:</span><span>₹${fee}</span></div>
            <div style="display:flex; justify-content:space-between; font-weight:bold; border-top:1px solid #ddd; margin-top:4px; padding-top:4px; color:#000;"><span>Total:</span><span>₹${total}</span></div>
        `;
        tdDetails.appendChild(breakdown);

        tr.appendChild(tdDetails);

        const tdStatus = document.createElement('td');
        tdStatus.setAttribute('data-label', 'Status');
        const spanStatus = document.createElement('span');
        spanStatus.className = `status-badge status-${order.status}`;
        spanStatus.textContent = order.status;
        tdStatus.appendChild(spanStatus);
        tr.appendChild(tdStatus);

        const tdAssign = document.createElement('td');
        tdAssign.setAttribute('data-label', 'Assigned To');
        if (isHistory) {
             tdAssign.textContent = order.assignedTo || 'Unassigned';
        } else {
            const assigneeSelect = document.createElement('select');
            const defaultOpt = document.createElement('option');
            defaultOpt.value = "";
            defaultOpt.textContent = "Unassigned";
            assigneeSelect.appendChild(defaultOpt);

            this.staffList.forEach(name => {
                const opt = document.createElement('option');
                opt.value = name;
                opt.textContent = name;
                if (order.assignedTo === name) opt.selected = true;
                assigneeSelect.appendChild(opt);
            });
            assigneeSelect.onchange = (e) => AdminApp.updateAssignee(docId, e.target.value);
            tdAssign.appendChild(assigneeSelect);
        }
        tr.appendChild(tdAssign);

        const tdActions = document.createElement('td');
        tdActions.setAttribute('data-label', 'Actions');
        if (isHistory) {
            const btnDel = document.createElement('button');
            btnDel.className = 'btn-small btn-danger';
            btnDel.textContent = 'Delete';
            btnDel.onclick = () => AdminApp.deleteSingleOrder(docId);
            tdActions.appendChild(btnDel);
        } else {
            const statuses = ['PENDING', 'PREPARING', 'READY_FOR_DELIVERY', 'REJECTED'];
            const statusSelect = document.createElement('select');
            statusSelect.onchange = (e) => AdminApp.updateStatus(docId, e.target.value);

            statuses.forEach(s => {
                const opt = document.createElement('option');
                opt.value = s;
                opt.textContent = s;
                if (s === order.status) opt.selected = true;
                statusSelect.appendChild(opt);
            });

            if (!statuses.includes(order.status)) {
                 const opt = document.createElement('option');
                 opt.value = order.status;
                 opt.textContent = order.status;
                 opt.selected = true;
                 opt.disabled = true;
                 statusSelect.prepend(opt);
            }

            tdActions.appendChild(statusSelect);
        }
        tr.appendChild(tdActions);

        return tr;
    },

    updateStatus: function(orderId, newStatus) {
        db.collection('orders').doc(orderId).update({ status: newStatus });
    },

    updateAssignee: function(orderId, staffName) {
        db.collection('orders').doc(orderId).update({ assignedTo: staffName });
    },

    deleteSingleOrder: function(orderId) {
        if(confirm('Are you sure you want to delete this order permanently?')) {
            db.collection('orders').doc(orderId).collection('private').doc('data').delete().then(() => {
                 db.collection('orders').doc(orderId).delete();
            }).catch(err => {
                 console.error('Error deleting subcollection, trying main doc:', err);
                 db.collection('orders').doc(orderId).delete();
            });
        }
    },

    deleteDeliveredOrders: function(mode) {
        const ordersRef = db.collection('orders');
        let query = ordersRef.where('status', '==', 'DELIVERED');

        if (mode === 'filter') {
            const start = document.getElementById('start-date').valueAsNumber;
            const end = document.getElementById('end-date').valueAsNumber;
            if (!start || !end) return alert('Select dates');

            const endDate = new Date(end);
            endDate.setHours(23, 59, 59, 999);

            query = query.where('orderDate', '>=', start).where('orderDate', '<=', endDate.getTime());
        }

        if (!confirm('Are you sure you want to delete these orders?')) return;

        query.get().then(async snapshot => {
            if (snapshot.empty) {
                alert('No orders found to delete.');
                return;
            }

            const batches = [];
            let batch = db.batch();
            let count = 0;

            for (const doc of snapshot.docs) {
                const privateRef = doc.ref.collection('private').doc('data');
                batch.delete(privateRef);

                batch.delete(doc.ref);
                count += 2;

                if (count >= 450) {
                    batches.push(batch.commit());
                    batch = db.batch();
                    count = 0;
                }
            }
            if (count > 0) batches.push(batch.commit());

            await Promise.all(batches);
            alert('Deleted ' + snapshot.size + ' delivered orders.');
        }).catch(err => {
            console.error(err);
            alert('Error deleting orders: ' + err.message);
        });
    },

    // --- MENU MANAGEMENT ---

    loadMenuManagement: function() {
        if (this.categoriesUnsubscribe) this.categoriesUnsubscribe();
        if (this.menuItemsUnsubscribe) this.menuItemsUnsubscribe();

        // Listener for Categories
        this.categoriesUnsubscribe = db.collection('categories').orderBy('order').onSnapshot(snap => {
            this.categories = [];
            snap.forEach(doc => {
                const cat = doc.data();
                cat.id = doc.id;
                this.categories.push(cat);
            });
            this.renderMenu();
        });

        // Listener for All Menu Items
        // Assuming menu size is reasonable to load all at once.
        this.menuItemsUnsubscribe = db.collection('menu_items').onSnapshot(snap => {
            this.menuItems = [];
            snap.forEach(doc => {
                const item = doc.data();
                item.id = doc.id;
                this.menuItems.push(item);
            });
            this.renderMenu();
        });
    },

    renderMenu: function() {
        const container = document.getElementById('menu-management-container');
        if (!container) return;

        container.innerHTML = '';

        if (this.categories.length === 0) {
            container.innerHTML = '<p>No categories found.</p>';
            return;
        }

        this.categories.forEach(cat => {
            // Category Block
            const block = document.createElement('div');
            block.className = 'category-block';

            // Header
            const header = document.createElement('div');
            header.className = 'category-header';

            const titleGroup = document.createElement('div');
            titleGroup.style.display = 'flex';
            titleGroup.style.alignItems = 'center';
            titleGroup.style.gap = '10px';
            titleGroup.style.cursor = 'pointer';

            const toggleIcon = document.createElement('span');
            toggleIcon.className = 'toggle-icon';
            toggleIcon.textContent = '▶'; // Default collapsed

            const title = document.createElement('h4');
            title.textContent = `${cat.name} (Order: ${cat.order})`;
            title.style.margin = '0';

            titleGroup.appendChild(toggleIcon);
            titleGroup.appendChild(title);

            // Header Actions
            const actions = document.createElement('div');

            const btnEdit = document.createElement('button');
            btnEdit.className = 'btn-small';
            btnEdit.textContent = 'Edit Cat';
            btnEdit.style.marginRight = '5px';
            btnEdit.onclick = (e) => { e.stopPropagation(); AdminApp.editCategory(cat.id); };

            const btnDel = document.createElement('button');
            btnDel.className = 'btn-small btn-danger';
            btnDel.textContent = 'Delete Cat';
            btnDel.onclick = (e) => { e.stopPropagation(); AdminApp.deleteCategory(cat.id); };

            actions.appendChild(btnEdit);
            actions.appendChild(btnDel);

            header.appendChild(titleGroup);
            header.appendChild(actions);

            // Items Container (Hidden by default)
            const itemsContainer = document.createElement('div');
            itemsContainer.className = 'category-items';
            itemsContainer.style.display = 'none';

            // Filter items for this category
            // Handle both category IDs (new format) and category names (legacy format)
            const catItems = this.menuItems.filter(item => item.category === cat.id || item.category === cat.name);

            if (catItems.length === 0) {
                itemsContainer.innerHTML = '<p style="padding: 10px; color: #666;">No items in this category.</p>';
            } else {
                const table = document.createElement('table');
                table.style.marginTop = '0'; // Override default table margin
                table.innerHTML = '<thead><tr><th>Name</th><th>Price</th><th>Description</th><th>Actions</th></tr></thead>';
                const tbody = document.createElement('tbody');

                catItems.forEach(item => {
                    const tr = document.createElement('tr');

                    const tdName = document.createElement('td');
                    tdName.setAttribute('data-label', 'Name');
                    tdName.textContent = item.name;
                    tr.appendChild(tdName);

                    const tdPrice = document.createElement('td');
                    tdPrice.setAttribute('data-label', 'Price');
                    tdPrice.textContent = item.price;
                    tr.appendChild(tdPrice);

                    const tdDesc = document.createElement('td');
                    tdDesc.setAttribute('data-label', 'Description');
                    tdDesc.textContent = item.description || '-';
                    tr.appendChild(tdDesc);

                    const tdActions = document.createElement('td');
                    tdActions.setAttribute('data-label', 'Actions');

                    const btnItemEdit = document.createElement('button');
                    btnItemEdit.className = 'btn-small';
                    btnItemEdit.textContent = 'Edit';
                    btnItemEdit.onclick = () => AdminApp.editItem(item.id, encodeURIComponent(JSON.stringify(item)));

                    const btnItemDel = document.createElement('button');
                    btnItemDel.className = 'btn-small btn-danger';
                    btnItemDel.textContent = 'Delete';
                    btnItemDel.style.marginLeft = '5px';
                    btnItemDel.onclick = () => AdminApp.deleteItem(item.id);

                    tdActions.appendChild(btnItemEdit);
                    tdActions.appendChild(btnItemDel);
                    tr.appendChild(tdActions);

                    tbody.appendChild(tr);
                });
                table.appendChild(tbody);
                itemsContainer.appendChild(table);
            }

            // Toggle Logic
            header.onclick = (e) => {
                // Ignore clicks on buttons inside header
                if (['BUTTON'].includes(e.target.tagName)) return;

                const isHidden = itemsContainer.style.display === 'none';
                itemsContainer.style.display = isHidden ? 'block' : 'none';
                toggleIcon.textContent = isHidden ? '▼' : '▶';
            };

            block.appendChild(header);
            block.appendChild(itemsContainer);
            container.appendChild(block);
        });
    },

    saveCategory: function() {
        const id = document.getElementById('cat-id').value;
        const name = document.getElementById('cat-name').value;
        const order = parseInt(document.getElementById('cat-order').value);

        if (id) {
            db.collection('categories').doc(id).update({ name, order });
        } else {
            db.collection('categories').add({ name, order });
        }
        closeModal('category-modal');
    },

    deleteCategory: function(id) {
        if (confirm('Delete category?')) {
            db.collection('categories').doc(id).delete();
        }
    },

    editCategory: function(id) {
        const cat = this.categories.find(c => c.id === id);
        document.getElementById('cat-id').value = id;
        document.getElementById('cat-name').value = cat.name;
        document.getElementById('cat-order').value = cat.order;
        document.getElementById('cat-modal-title').innerText = 'Edit Category';
        openModal('category-modal');
    },

    saveMenuItem: function() {
        const id = document.getElementById('item-id').value;
        const catId = document.getElementById('item-category').value;
        const data = {
            name: document.getElementById('item-name').value,
            description: document.getElementById('item-desc').value,
            price: parseFloat(document.getElementById('item-price').value),
            imageUrl: document.getElementById('item-image').value,
            category: catId
        };

        if (id) {
            db.collection('menu_items').doc(id).update(data);
        } else {
            db.collection('menu_items').add(data);
        }
        closeModal('item-modal');
    },

    editItem: function(id, itemStr) {
        const item = JSON.parse(decodeURIComponent(itemStr));
        document.getElementById('item-id').value = id;
        document.getElementById('item-name').value = item.name;
        document.getElementById('item-desc').value = item.description;
        document.getElementById('item-price').value = item.price;
        document.getElementById('item-image').value = item.imageUrl;

        // Ensure category select is populated
        this.populateCategorySelect('item-category');

        // Handle both category ID and legacy category Name
        // Find the category object that matches either by ID or Name
        const matchedCat = this.categories.find(c => c.id === item.category || c.name === item.category);
        if (matchedCat) {
             document.getElementById('item-category').value = matchedCat.id;
        } else {
             // Fallback if no match found (shouldn't happen if categories exist)
             document.getElementById('item-category').value = item.category;
        }

        document.getElementById('item-modal-title').innerText = 'Edit Item';
        openModal('item-modal');
    },

    deleteItem: function(id) {
        if (confirm('Delete item?')) db.collection('menu_items').doc(id).delete();
    },

    openItemModal: function() {
        document.getElementById('item-id').value = '';
        document.getElementById('item-name').value = '';
        document.getElementById('item-desc').value = '';
        document.getElementById('item-price').value = '';
        document.getElementById('item-image').value = '';

        this.populateCategorySelect('item-category');

        document.getElementById('item-modal-title').innerText = 'Add Item';
        openModal('item-modal');
    },

    populateCategorySelect: function(elementId) {
        const select = document.getElementById(elementId);
        select.innerHTML = '';
        this.categories.forEach(cat => {
            const opt = document.createElement('option');
            opt.value = cat.id;
            opt.textContent = cat.name;
            select.appendChild(opt);
        });
    },

    // --- CSV ---
    downloadCsv: function() {
        // Use local data instead of fetching again if possible, or just fetch
        Promise.all([
            db.collection('categories').get(),
            db.collection('menu_items').get()
        ]).then(([catSnap, itemSnap]) => {
            const catMap = {};
            catSnap.forEach(doc => catMap[doc.id] = doc.data().name);

            let csv = 'Category,Name,Description,Price,ImageURL\n';
            itemSnap.forEach(doc => {
                const item = doc.data();
                const catName = catMap[item.category] || item.category;
                const name = `"${(item.name || '').replace(/"/g, '""')}"`;
                const desc = `"${(item.description || '').replace(/"/g, '""')}"`;
                csv += `${catName},${name},${desc},${item.price},${item.imageUrl}\n`;
            });

            const blob = new Blob([csv], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'menu.csv';
            a.click();
        });
    },

    uploadCsv: function(input) {
        const file = input.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = async (e) => {
            const text = e.target.result;
            const lines = text.split('\n');

            const catSnap = await db.collection('categories').get();
            let catMap = {};
            catSnap.forEach(doc => catMap[doc.data().name] = doc.id);

            let batch = db.batch();
            let ops = 0;

            for (let i = 1; i < lines.length; i++) {
                if (!lines[i].trim()) continue;
                const row = lines[i].match(/(".*?"|[^",]+)(?=\s*,|\s*$)/g);
                if (!row) continue;

                const clean = (val) => val ? val.replace(/^"|"$/g, '').replace(/""/g, '"').trim() : '';

                const catName = clean(row[0]);
                const name = clean(row[1]);
                const desc = clean(row[2]);
                const price = parseFloat(clean(row[3]));
                const img = clean(row[4]);

                if (!catName || !name) continue;

                let catId = catMap[catName];
                if (!catId) {
                    const newCatRef = db.collection('categories').doc();
                    batch.set(newCatRef, { name: catName, order: 99 });
                    catId = newCatRef.id;
                    catMap[catName] = catId;
                    ops++;
                }

                const itemRef = db.collection('menu_items').doc();
                batch.set(itemRef, {
                    name: name,
                    description: desc,
                    price: price,
                    imageUrl: img,
                    category: catId
                });
                ops++;

                if (ops > 450) {
                     await batch.commit();
                     batch = db.batch();
                     ops = 0;
                }
            }
            if (ops > 0) await batch.commit();
            alert('Menu uploaded successfully');
            // No need to manually reload, listeners will catch up
            input.value = '';
        };
        reader.readAsText(file);
    },

    showSection: function(sectionId) {
        document.querySelectorAll('.section').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('nav button').forEach(el => el.classList.remove('active'));
        document.getElementById(sectionId + '-section').classList.add('active');
        document.getElementById('nav-' + sectionId).classList.add('active');
    }
};
