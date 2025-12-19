// App Logic

// Check Auth
auth.onAuthStateChanged(user => {
    if (user) {
        // Verify if admin
        db.collection('admins').doc(user.uid).get().then(doc => {
            if (doc.exists || user.email === 'admin@pizzaparadize.com') { // Fallback for initial admin
                document.getElementById('auth-container').style.display = 'none';
                document.getElementById('dashboard-container').style.display = 'block';
                loadOrders();
                loadCategories();
            } else {
                alert('Access Denied. Not an admin.');
                auth.signOut();
            }
        });
    } else {
        document.getElementById('auth-container').style.display = 'block';
        document.getElementById('dashboard-container').style.display = 'none';
    }
});

function login() {
    const email = document.getElementById('email').value;
    const pass = document.getElementById('password').value;
    auth.signInWithEmailAndPassword(email, pass).catch(err => {
        document.getElementById('login-error').innerText = err.message;
    });
}

function logout() {
    auth.signOut();
}

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('nav button').forEach(el => el.classList.remove('active'));
    document.getElementById(sectionId + '-section').classList.add('active');
    document.getElementById('nav-' + sectionId).classList.add('active');
}

// --- ORDERS ---

function loadOrders() {
    db.collection('orders').orderBy('orderDate', 'desc').onSnapshot(snapshot => {
        const list = document.getElementById('orders-list');
        list.innerHTML = '<table><thead><tr><th>Date</th><th>Customer</th><th>Status</th><th>Total</th><th>Assigned To</th><th>Actions</th></tr></thead><tbody id="orders-table-body"></tbody></table>';
        const tbody = document.getElementById('orders-table-body');

        snapshot.forEach(doc => {
            const order = doc.data();
            const date = new Date(order.orderDate).toLocaleString();
            const tr = document.createElement('tr');

            // Status Select
            const statuses = ['PENDING', 'PREPARING', 'READY_FOR_DELIVERY', 'PICKED_UP', 'DELIVERED', 'REJECTED'];
            let statusOptions = statuses.map(s => `<option value="${s}" ${s === order.status ? 'selected' : ''}>${s}</option>`).join('');

            tr.innerHTML = `
                <td>${date}</td>
                <td>${order.customerName}<br><small>${order.deliveryAddress}</small></td>
                <td><span class="status-badge status-${order.status}">${order.status}</span></td>
                <td>${order.grandTotal.toFixed(2)}</td>
                <td><input type="text" value="${order.assignedTo || ''}" placeholder="Staff Name" onchange="updateAssignee('${doc.id}', this.value)"></td>
                <td>
                    <select onchange="updateStatus('${doc.id}', this.value)">${statusOptions}</select>
                </td>
            `;
            tbody.appendChild(tr);
        });
    });
}

function updateStatus(orderId, newStatus) {
    db.collection('orders').doc(orderId).update({ status: newStatus });
}

function updateAssignee(orderId, staffName) {
    db.collection('orders').doc(orderId).update({ assignedTo: staffName });
}

function deleteDeliveredOrders(mode) {
    const ordersRef = db.collection('orders');
    let query = ordersRef.where('status', '==', 'DELIVERED');

    if (mode === 'filter') {
        const start = document.getElementById('start-date').valueAsNumber;
        const end = document.getElementById('end-date').valueAsNumber; // This gives midnight UTC usually
        if (!start || !end) return alert('Select dates');

        // Adjust end date to cover the full day
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
}


// --- MENU ---

let categories = [];
let menuItemsUnsubscribe = null;

function loadCategories() {
    db.collection('categories').orderBy('order').onSnapshot(snap => {
        categories = [];
        const list = document.getElementById('categories-list');
        list.innerHTML = '<table><thead><tr><th>Order</th><th>Name</th><th>Actions</th></tr></thead><tbody id="cat-body"></tbody></table>';
        const tbody = document.getElementById('cat-body');
        const filter = document.getElementById('category-filter');
        const itemCatSelect = document.getElementById('item-category');

        filter.innerHTML = '<option value="">Select Category</option>';
        itemCatSelect.innerHTML = '';

        snap.forEach(doc => {
            const cat = doc.data();
            cat.id = doc.id;
            categories.push(cat);

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${cat.order}</td>
                <td>${cat.name}</td>
                <td>
                    <button class="btn-small" onclick="editCategory('${cat.id}')">Edit</button>
                    <button class="btn-small btn-danger" onclick="deleteCategory('${cat.id}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);

            const opt = `<option value="${cat.id}">${cat.name}</option>`;
            filter.innerHTML += opt;
            itemCatSelect.innerHTML += opt;
        });
    });
}

function saveCategory() {
    const id = document.getElementById('cat-id').value;
    const name = document.getElementById('cat-name').value;
    const order = parseInt(document.getElementById('cat-order').value);

    if (id) {
        db.collection('categories').doc(id).update({ name, order });
    } else {
        db.collection('categories').add({ name, order });
    }
    closeModal('category-modal');
}

function deleteCategory(id) {
    if (confirm('Delete category?')) {
        db.collection('categories').doc(id).delete();
    }
}

function editCategory(id) {
    const cat = categories.find(c => c.id === id);
    document.getElementById('cat-id').value = id;
    document.getElementById('cat-name').value = cat.name;
    document.getElementById('cat-order').value = cat.order;
    document.getElementById('cat-modal-title').innerText = 'Edit Category';
    openModal('category-modal');
}


function loadMenuItems() {
    const catId = document.getElementById('category-filter').value;
    if (!catId) return;

    if (menuItemsUnsubscribe) {
        menuItemsUnsubscribe();
    }

    menuItemsUnsubscribe = db.collection('menu_items').where('category', '==', catId).onSnapshot(snap => {
        const list = document.getElementById('menu-items-list');
        list.innerHTML = '<table><thead><tr><th>Name</th><th>Price</th><th>Actions</th></tr></thead><tbody id="items-body"></tbody></table>';
        const tbody = document.getElementById('items-body');

        snap.forEach(doc => {
            const item = doc.data();
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${item.name}</td>
                <td>${item.price}</td>
                <td>
                    <button class="btn-small" onclick="editItem('${doc.id}', '${encodeURIComponent(JSON.stringify(item))}')">Edit</button>
                    <button class="btn-small btn-danger" onclick="deleteItem('${doc.id}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    });
}

function saveMenuItem() {
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
}

function editItem(id, itemStr) {
    const item = JSON.parse(decodeURIComponent(itemStr));
    document.getElementById('item-id').value = id;
    document.getElementById('item-category').value = item.category;
    document.getElementById('item-name').value = item.name;
    document.getElementById('item-desc').value = item.description;
    document.getElementById('item-price').value = item.price;
    document.getElementById('item-image').value = item.imageUrl;

    document.getElementById('item-modal-title').innerText = 'Edit Item';
    openModal('item-modal');
}

function deleteItem(id) {
    if (confirm('Delete item?')) db.collection('menu_items').doc(id).delete();
}

function openItemModal() {
    document.getElementById('item-id').value = '';
    document.getElementById('item-name').value = '';
    document.getElementById('item-desc').value = '';
    document.getElementById('item-price').value = '';
    document.getElementById('item-image').value = '';
    // Set category to current filter if selected
    const currentFilter = document.getElementById('category-filter').value;
    if (currentFilter) document.getElementById('item-category').value = currentFilter;

    document.getElementById('item-modal-title').innerText = 'Add Item';
    openModal('item-modal');
}

// --- CSV ---

function downloadCsv() {
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
            // Escape quotes
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
}

function uploadCsv(input) {
    const file = input.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (e) => {
        const text = e.target.result;
        const lines = text.split('\n');
        const headers = lines[0].split(','); // Assume simple CSV for now

        // Cache categories
        const catSnap = await db.collection('categories').get();
        let catMap = {}; // Name -> ID
        catSnap.forEach(doc => catMap[doc.data().name] = doc.id);

        const batch = db.batch();
        let ops = 0;

        for (let i = 1; i < lines.length; i++) {
            if (!lines[i].trim()) continue;
            // Simple comma split (WARNING: breaks if commas in fields, but using regex for CSV parsing is hefty for vanilla JS without libs. I'll stick to simple split or basic regex).
            // Better regex for CSV:
            const matches = lines[i].match(/(".*?"|[^",\s]+)(?=\s*,|\s*$)/g);
            // This is complex. Let's assume standard "Category","Name","Desc",Price,Url format or simple split if no quotes.

            // Simplified logic: user provided CSV must be clean or I use a simple parser.
            // Let's use a basic regex split that handles quotes.
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
                // Create category if missing? This is async inside loop, difficult for batch.
                // For simplicity, skip or error. Or separate category creation pass.
                // Let's create it.
                const newCatRef = db.collection('categories').doc();
                batch.set(newCatRef, { name: catName, order: 99 });
                catId = newCatRef.id;
                catMap[catName] = catId;
                ops++;
            }

            const itemRef = db.collection('menu_items').doc(); // Create new items (avoid dupe check for simplicity or use name query?)
            // Ideally should check if item exists.
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
                 ops = 0;
            }
        }
        if (ops > 0) await batch.commit();
        alert('Menu uploaded successfully');
        loadCategories(); // Refresh
        input.value = '';
    };
    reader.readAsText(file);
}


// UI Helpers
function openModal(id) { document.getElementById(id).style.display = 'block'; }
function closeModal(id) { document.getElementById(id).style.display = 'none'; }
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) event.target.style.display = 'none';
}
