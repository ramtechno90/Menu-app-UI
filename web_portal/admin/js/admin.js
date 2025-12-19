// Admin Logic Namespace
const AdminApp = {
    categories: [],
    staffList: [],
    menuItemsUnsubscribe: null,
    ordersUnsubscribe: null,

    init: function() {
        console.log('Initializing Admin Dashboard');
        this.loadCategories();
        this.loadDeliveryStaff().then(() => {
            this.loadOrders();
        });
    },

    cleanup: function() {
        console.log('Cleaning up Admin Dashboard');
        if (this.ordersUnsubscribe) this.ordersUnsubscribe();
        if (this.menuItemsUnsubscribe) this.menuItemsUnsubscribe();
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

        this.ordersUnsubscribe = db.collection('orders').orderBy('orderDate', 'desc').onSnapshot(snapshot => {
            const list = document.getElementById('orders-list');
            if (!list) return; // UI not present?

            list.innerHTML = '';
            const table = document.createElement('table');
            table.innerHTML = '<thead><tr><th>Date</th><th>Customer</th><th>Details</th><th>Status</th><th>Assigned To</th><th>Actions</th></tr></thead>';
            const tbody = document.createElement('tbody');
            tbody.id = 'orders-table-body';

            snapshot.forEach(doc => {
                const order = doc.data();
                const date = new Date(order.orderDate).toLocaleString();
                const tr = document.createElement('tr');

                const statuses = ['PENDING', 'PREPARING', 'READY_FOR_DELIVERY', 'PICKED_UP', 'DELIVERED', 'REJECTED'];
                const statusSelect = document.createElement('select');
                statusSelect.onchange = (e) => AdminApp.updateStatus(doc.id, e.target.value);
                statuses.forEach(s => {
                    const opt = document.createElement('option');
                    opt.value = s;
                    opt.textContent = s;
                    if (s === order.status) opt.selected = true;
                    statusSelect.appendChild(opt);
                });

                // Assignee Dropdown
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
                assigneeSelect.onchange = (e) => AdminApp.updateAssignee(doc.id, e.target.value);


                // Date
                const tdDate = document.createElement('td');
                tdDate.textContent = date;
                tr.appendChild(tdDate);

                // Customer
                const tdCustomer = document.createElement('td');
                const divCust = document.createElement('div');
                divCust.textContent = order.customerName;
                const smallAddr = document.createElement('small');
                smallAddr.textContent = order.deliveryAddress;
                smallAddr.style.display = 'block';
                tdCustomer.appendChild(divCust);
                tdCustomer.appendChild(smallAddr);
                tr.appendChild(tdCustomer);

                // Details (Items + Calculation)
                const tdDetails = document.createElement('td');
                const itemList = document.createElement('ul');
                itemList.style.paddingLeft = '20px';
                itemList.style.marginBottom = '5px';

                if (order.cartItems && Array.isArray(order.cartItems)) {
                    order.cartItems.forEach(item => {
                        const li = document.createElement('li');
                        let text = `${item.name} x ${item.quantity}`;
                        if (item.notes) {
                            text += ` (Note: ${item.notes})`;
                        }
                        li.textContent = text;
                        itemList.appendChild(li);
                    });
                }
                tdDetails.appendChild(itemList);

                const calcDiv = document.createElement('div');
                calcDiv.style.fontSize = '0.9em';
                calcDiv.style.borderTop = '1px solid #eee';
                calcDiv.style.paddingTop = '5px';

                // Fallback for older orders or if fields missing
                const sub = order.subtotal !== undefined ? order.subtotal.toFixed(2) : '?';
                const tax = order.tax !== undefined ? order.tax.toFixed(2) : '?';
                const del = order.deliveryFee !== undefined ? order.deliveryFee.toFixed(2) : '?';
                const tot = order.grandTotal !== undefined ? order.grandTotal.toFixed(2) : '?';

                calcDiv.innerHTML = `Sub: ${sub} + Tax: ${tax} + Del: ${del} = <strong>${tot}</strong>`;
                tdDetails.appendChild(calcDiv);

                tr.appendChild(tdDetails);

                // Status
                const tdStatus = document.createElement('td');
                const spanStatus = document.createElement('span');
                spanStatus.className = `status-badge status-${order.status}`;
                spanStatus.textContent = order.status;
                tdStatus.appendChild(spanStatus);
                tr.appendChild(tdStatus);

                // Assignee
                const tdAssign = document.createElement('td');
                tdAssign.appendChild(assigneeSelect);
                tr.appendChild(tdAssign);

                // Actions
                const tdActions = document.createElement('td');
                tdActions.appendChild(statusSelect);
                tr.appendChild(tdActions);

                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            list.appendChild(table);
        });
    },

    updateStatus: function(orderId, newStatus) {
        db.collection('orders').doc(orderId).update({ status: newStatus });
    },

    updateAssignee: function(orderId, staffName) {
        db.collection('orders').doc(orderId).update({ assignedTo: staffName });
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

            snapshot.docs.forEach(doc => {
                batch.delete(doc.ref);
                count++;
                if (count >= 450) {
                    batches.push(batch.commit());
                    batch = db.batch();
                    count = 0;
                }
            });
            if (count > 0) batches.push(batch.commit());

            await Promise.all(batches);
            alert('Deleted ' + snapshot.size + ' delivered orders.');
        }).catch(err => {
            console.error(err);
            alert('Error deleting orders: ' + err.message);
        });
    },

    // --- MENU ---
    loadCategories: function() {
        this.catUnsubscribe = db.collection('categories').orderBy('order').onSnapshot(snap => {
            this.categories = [];
            const list = document.getElementById('categories-list');
            const filter = document.getElementById('category-filter');
            const itemCatSelect = document.getElementById('item-category');

            if (!list) return;

            list.innerHTML = '';
            filter.innerHTML = '<option value="">Select Category</option>';
            itemCatSelect.innerHTML = '<option value="">Select Category</option>';

            const table = document.createElement('table');
            table.innerHTML = '<thead><tr><th>Order</th><th>Name</th><th>Actions</th></tr></thead>';
            const tbody = document.createElement('tbody');

            snap.forEach(doc => {
                const cat = doc.data();
                cat.id = doc.id;
                this.categories.push(cat);

                const tr = document.createElement('tr');

                const tdOrder = document.createElement('td');
                tdOrder.textContent = cat.order;
                tr.appendChild(tdOrder);

                const tdName = document.createElement('td');
                tdName.textContent = cat.name;
                tr.appendChild(tdName);

                const tdActions = document.createElement('td');

                const btnEdit = document.createElement('button');
                btnEdit.className = 'btn-small';
                btnEdit.textContent = 'Edit';
                btnEdit.onclick = () => AdminApp.editCategory(cat.id);
                tdActions.appendChild(btnEdit);

                // Spacer
                tdActions.appendChild(document.createTextNode(' '));

                const btnDel = document.createElement('button');
                btnDel.className = 'btn-small btn-danger';
                btnDel.textContent = 'Delete';
                btnDel.onclick = () => AdminApp.deleteCategory(cat.id);
                tdActions.appendChild(btnDel);

                tr.appendChild(tdActions);
                tbody.appendChild(tr);

                // Update selects
                const opt1 = document.createElement('option');
                opt1.value = cat.id;
                opt1.textContent = cat.name;
                filter.appendChild(opt1);

                const opt2 = document.createElement('option');
                opt2.value = cat.id;
                opt2.textContent = cat.name;
                itemCatSelect.appendChild(opt2);
            });

            table.appendChild(tbody);
            list.appendChild(table);
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

    loadMenuItems: function() {
        const catId = document.getElementById('category-filter').value;
        if (!catId) return;

        if (this.menuItemsUnsubscribe) {
            this.menuItemsUnsubscribe();
        }

        this.menuItemsUnsubscribe = db.collection('menu_items').where('category', '==', catId).onSnapshot(snap => {
            const list = document.getElementById('menu-items-list');
            list.innerHTML = '';

            const table = document.createElement('table');
            table.innerHTML = '<thead><tr><th>Name</th><th>Price</th><th>Actions</th></tr></thead>';
            const tbody = document.createElement('tbody');

            snap.forEach(doc => {
                const item = doc.data();
                const tr = document.createElement('tr');

                const tdName = document.createElement('td');
                tdName.textContent = item.name;
                tr.appendChild(tdName);

                const tdPrice = document.createElement('td');
                tdPrice.textContent = item.price;
                tr.appendChild(tdPrice);

                const tdActions = document.createElement('td');

                const btnEdit = document.createElement('button');
                btnEdit.className = 'btn-small';
                btnEdit.textContent = 'Edit';
                btnEdit.onclick = () => AdminApp.editItem(doc.id, encodeURIComponent(JSON.stringify(item)));
                tdActions.appendChild(btnEdit);

                tdActions.appendChild(document.createTextNode(' '));

                const btnDel = document.createElement('button');
                btnDel.className = 'btn-small btn-danger';
                btnDel.textContent = 'Delete';
                btnDel.onclick = () => AdminApp.deleteItem(doc.id);
                tdActions.appendChild(btnDel);

                tr.appendChild(tdActions);
                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            list.appendChild(table);
        });
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
        document.getElementById('item-category').value = item.category;
        document.getElementById('item-name').value = item.name;
        document.getElementById('item-desc').value = item.description;
        document.getElementById('item-price').value = item.price;
        document.getElementById('item-image').value = item.imageUrl;

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
        const currentFilter = document.getElementById('category-filter').value;
        if (currentFilter) document.getElementById('item-category').value = currentFilter;

        document.getElementById('item-modal-title').innerText = 'Add Item';
        openModal('item-modal');
    },

    // --- CSV ---
    downloadCsv: function() {
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
            this.loadCategories();
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
