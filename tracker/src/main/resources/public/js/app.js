const state = {
    customers: [],
    devices: [],
    jobs: [],
    selectedJobId: null
};

const dashboardEls = {
    totalJobs: document.getElementById('totalJobs'),
    jobsInProgress: document.getElementById('jobsInProgress'),
    jobsReadyForCollection: document.getElementById('jobsReadyForCollection'),
    healthStatus: document.getElementById('healthStatus')
};

const tableBody = document.getElementById('jobsTableBody');
const searchInput = document.getElementById('searchInput');
const customerForm = document.getElementById('customerForm');
const deviceForm = document.getElementById('deviceForm');
const jobForm = document.getElementById('jobForm');
const jobDetailEmpty = document.getElementById('jobDetailEmpty');
const jobDetailContent = document.getElementById('jobDetailContent');
const repairUpdateForm = document.getElementById('repairUpdateForm');
const statusSelect = document.getElementById('statusSelect');
const paymentStatusForm = document.getElementById('paymentStatusForm');
const paymentStatusSelect = document.getElementById('paymentStatusSelect');
const outcomeSelect = document.getElementById('outcomeSelect');
const customerList = document.getElementById('customerList');
const deviceList = document.getElementById('deviceList');

async function fetchJson(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        },
        ...options
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : {};
    if (!response.ok) {
        const message = data.message || 'Something went wrong';
        throw new Error(message);
    }
    return data;
}

async function checkHealth() {
    try {
        const health = await fetchJson('/api/health');
        dashboardEls.healthStatus.textContent = `DB: ${health.databaseName || 'connected'}`;
    } catch (error) {
        dashboardEls.healthStatus.textContent = 'DB offline';
    }
}

async function loadDashboard() {
    const data = await fetchJson('/api/dashboard');
    dashboardEls.totalJobs.textContent = data.totalJobs ?? 0;
    dashboardEls.jobsInProgress.textContent = data.jobsInProgress ?? 0;
    dashboardEls.jobsReadyForCollection.textContent = data.jobsReadyForCollection ?? 0;
}

async function loadCustomers() {
    const customers = await fetchJson('/api/customers');
    state.customers = customers;
    populateCustomerSelects();
    renderInventory();
}

async function loadDevices() {
    state.devices = await fetchJson('/api/devices');
    renderInventory();
}

function renderInventory() {
    document.getElementById('customerCount').textContent = state.customers.length;
    document.getElementById('deviceCount').textContent = state.devices.length;

    customerList.innerHTML = state.customers.length
        ? state.customers.map(customer => `
            <div class="inventory-item">
                <div>
                    <strong>${customer.fullName}</strong>
                    <span>${customer.phoneNumber}</span>
                </div>
                <div class="inventory-actions">
                    <span class="badge">Customer #${customer.id}</span>
                    <button type="button" class="secondary-button" data-action="edit-customer" data-id="${customer.id}">Edit</button>
                    <button type="button" class="danger-button" data-action="delete-customer" data-id="${customer.id}">Delete</button>
                </div>
            </div>
        `).join('')
        : '<div class="empty-state">No customers created yet.</div>';

    deviceList.innerHTML = state.devices.length
        ? state.devices.map(device => {
            const customer = state.customers.find(item => item.id === device.customerId);
            return `
                <div class="inventory-item">
                    <div>
                        <strong>${device.brand} ${device.model}</strong>
                        <span>${device.deviceType} · ${customer ? customer.fullName : 'Unknown customer'}</span>
                    </div>
                    <div class="inventory-actions">
                        <span class="badge">Device #${device.id}</span>
                        <button type="button" class="secondary-button" data-action="edit-device" data-id="${device.id}">Edit</button>
                        <button type="button" class="danger-button" data-action="delete-device" data-id="${device.id}">Delete</button>
                    </div>
                </div>
            `;
        }).join('')
        : '<div class="empty-state">No devices registered yet.</div>';
}

async function editCustomer(id) {
    const customer = state.customers.find(item => item.id === id);
    if (!customer) return;
    const fullName = window.prompt('Customer full name:', customer.fullName);
    if (fullName === null) return;
    const phoneNumber = window.prompt('Customer phone number:', customer.phoneNumber);
    if (phoneNumber === null) return;

    await fetchJson(`/api/customers/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ fullName, phoneNumber })
    });
    await loadCustomers();
    await loadJobs();
}

async function editDevice(id) {
    const device = state.devices.find(item => item.id === id);
    if (!device) return;
    const customerId = window.prompt('Customer ID:', String(device.customerId));
    if (customerId === null) return;
    const deviceType = window.prompt('Device type:', device.deviceType);
    if (deviceType === null) return;
    const brand = window.prompt('Brand:', device.brand);
    if (brand === null) return;
    const model = window.prompt('Model:', device.model);
    if (model === null) return;

    await fetchJson(`/api/devices/${id}`, {
        method: 'PUT',
        body: JSON.stringify({
            customerId: Number(customerId),
            deviceType,
            brand,
            model
        })
    });
    await loadDevices();
    await loadJobs();
}

async function deleteInventoryItem(type, id) {
    const label = type === 'customer' ? 'customer' : 'device';
    if (!window.confirm(`Delete this ${label}? This cannot be undone.`)) return;
    await fetchJson(`/api/${type}s/${id}`, { method: 'DELETE' });
    await loadCustomers();
    await loadDevices();
    await loadJobs();
}

async function handleInventoryAction(event) {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const id = Number(button.dataset.id);
    try {
        if (button.dataset.action === 'edit-customer') {
            await editCustomer(id);
        } else if (button.dataset.action === 'edit-device') {
            await editDevice(id);
        } else if (button.dataset.action === 'delete-customer') {
            await deleteInventoryItem('customer', id);
        } else if (button.dataset.action === 'delete-device') {
            await deleteInventoryItem('device', id);
        }
    } catch (error) {
        alert(error.message);
    }
}

function populateCustomerSelects() {
    const customerOptions = state.customers
        .map(customer => `<option value="${customer.id}">${customer.fullName} (${customer.phoneNumber})</option>`)
        .join('');

    document.getElementById('deviceCustomerSelect').innerHTML = '<option value="">Select a customer</option>' + customerOptions;
    document.getElementById('jobCustomerSelect').innerHTML = '<option value="">Select a customer</option>' + customerOptions;

    const deviceSelect = document.getElementById('jobDeviceSelect');
    const customerId = document.getElementById('jobCustomerSelect').value;
    loadDevicesForCustomer(customerId, deviceSelect);
}

async function loadDevicesForCustomer(customerId, selectElement) {
    if (!customerId) {
        selectElement.innerHTML = '<option value="">Select a customer first</option>';
        return;
    }

    const devices = await fetchJson('/api/devices');
    const customerDevices = devices.filter(device => device.customerId === Number(customerId));
    selectElement.innerHTML = '<option value="">Select a device</option>' + customerDevices
        .map(device => `<option value="${device.id}">${device.brand} ${device.model}</option>`)
        .join('');
}

async function loadDeviceInventory() {
    try {
        const devices = await fetchJson('/api/devices');
        const jobDeviceSelect = document.getElementById('jobDeviceSelect');
        const selectedCustomerId = document.getElementById('jobCustomerSelect').value;
        const filtered = devices.filter(device => device.customerId === Number(selectedCustomerId));
        jobDeviceSelect.innerHTML = '<option value="">Select a device</option>' + filtered
            .map(device => `<option value="${device.id}">${device.brand} ${device.model}</option>`)
            .join('');
    } catch (error) {
        console.error(error);
    }
}

async function loadJobs() {
    const jobs = await fetchJson('/api/repair-jobs');
    state.jobs = jobs;
    renderJobs(jobs);

    if (state.selectedJobId) {
        const selected = jobs.find(job => job.id === state.selectedJobId);
        if (selected) {
            renderJobDetail(selected.id);
        }
    }
}

function renderJobs(jobs) {
    if (!jobs.length) {
        tableBody.innerHTML = '<tr><td colspan="10"><div class="empty-state">No repair jobs found.</div></td></tr>';
        return;
    }

    tableBody.innerHTML = jobs.map(job => {
        const customer = state.customers.find(customer => customer.id === job.customerId) || { fullName: 'Unknown', phoneNumber: 'N/A' };
        const device = getDeviceById(job.deviceId) || { brand: 'Unknown', model: 'Unknown' };
        const statusClass = job.status.replace(/_/g, '-').toLowerCase();
        return `
            <tr data-job-id="${job.id}" class="${job.id === state.selectedJobId ? 'selected' : ''}">
                <td>#${job.id}</td>
                <td>${customer.fullName}</td>
                <td>${customer.phoneNumber}</td>
                <td>${device.brand} ${device.model}</td>
                <td>${job.reportedFault}</td>
                <td><span class="badge ${statusClass}">${formatRepairStatus(job.status)}</span></td>
                <td>${job.repairOutcome.replace(/_/g, ' ')}</td>
                <td>${formatAmount(job.repairCost)}</td>
                <td>${formatPaymentStatus(job.paymentStatus)}</td>
                <td>${formatDate(job.createdAt)}</td>
            </tr>
        `;
    }).join('');

    tableBody.querySelectorAll('tr[data-job-id]').forEach(row => {
        row.addEventListener('click', () => {
            const jobId = Number(row.dataset.jobId);
            state.selectedJobId = jobId;
            tableBody.querySelectorAll('tr[data-job-id]').forEach(jobRow => {
                jobRow.classList.toggle('selected', Number(jobRow.dataset.jobId) === jobId);
            });
            renderJobDetail(jobId);
        });
    });
}

function getDeviceById(deviceId) {
    return state.devices.find(device => device.id === Number(deviceId)) || null;
}

async function renderJobDetail(jobId) {
    try {
        const result = await fetchJson(`/api/repair-jobs/${jobId}`);
        const { job, records, allowedNextStatuses } = result;
        const device = getDeviceById(job.deviceId) || { deviceType: 'Device', brand: 'Unknown', model: 'Unknown' };
        const customer = state.customers.find(item => item.id === job.customerId) || { fullName: 'Unknown customer' };
        jobDetailEmpty.classList.add('hidden');
        jobDetailContent.classList.remove('hidden');

        document.getElementById('selectedJobContext').textContent = `Job #${job.id} · ${customer.fullName} · ${device.deviceType} ${device.brand} ${device.model}`;
        document.getElementById('detailFault').textContent = job.reportedFault;
        document.getElementById('detailOutcome').textContent = job.repairOutcome.replace(/_/g, ' ');
        outcomeSelect.value = job.repairOutcome;
        document.getElementById('repairCostDisplay').textContent = formatAmount(job.repairCost);
        document.getElementById('paymentStatusDisplay').textContent = formatPaymentStatus(job.paymentStatus);
        paymentStatusSelect.value = job.paymentStatus;

        const statusLabels = {
            IN_PROGRESS: 'Not completed (In progress)',
            READY_FOR_COLLECTION: 'Ready for collection',
            COMPLETED: 'Completed'
        };
        statusSelect.innerHTML = '<option value="" selected>Keep current status</option>' +
            allowedNextStatuses.map(status => `<option value="${status}">${statusLabels[status]}</option>`).join('');

        const historyList = document.getElementById('historyList');
        if (!records.length) {
            historyList.innerHTML = '<li>No status changes recorded yet.</li>';
        } else {
            historyList.innerHTML = records.map(record => `<li>${record.newStatus} - ${record.notes || 'No note'} (${formatDate(record.recordedAt)})</li>`).join('');
        }
    } catch (error) {
        console.error(error);
    }
}

async function submitCustomerForm(event) {
    event.preventDefault();
    const formData = new FormData(customerForm);
    try {
        await fetchJson('/api/customers', {
            method: 'POST',
            body: JSON.stringify({
                fullName: formData.get('fullName'),
                phoneNumber: formData.get('phoneNumber')
            })
        });
        customerForm.reset();
        await loadCustomers();
        await loadDevices();
        await loadJobs();
    } catch (error) {
        alert(error.message);
    }
}

async function submitDeviceForm(event) {
    event.preventDefault();
    const formData = new FormData(deviceForm);
    try {
        const device = await fetchJson('/api/devices', {
            method: 'POST',
            body: JSON.stringify({
                customerId: Number(formData.get('customerId')),
                deviceType: formData.get('deviceType'),
                brand: formData.get('brand'),
                model: formData.get('model')
            })
        });
        deviceForm.reset();
        await loadCustomers();
        await loadDevices();
        await loadJobs();
    } catch (error) {
        alert(error.message);
    }
}

async function submitJobForm(event) {
    event.preventDefault();
    const formData = new FormData(jobForm);
    try {
        await fetchJson('/api/repair-jobs', {
            method: 'POST',
            body: JSON.stringify({
                customerId: Number(formData.get('customerId')),
                deviceId: Number(formData.get('deviceId')),
                reportedFault: formData.get('reportedFault'),
                repairNotes: formData.get('repairNotes'),
                repairCost: Number(formData.get('repairCost')),
                paymentStatus: formData.get('paymentStatus')
            })
        });
        jobForm.reset();
        await loadJobs();
        await loadDashboard();
    } catch (error) {
        alert(error.message);
    }
}

async function submitRepairUpdateForm(event) {
    event.preventDefault();
    if (!state.selectedJobId) {
        alert('Select a job first');
        return;
    }

    try {
        const status = statusSelect.value;
        if (status) {
            await fetchJson(`/api/repair-jobs/${state.selectedJobId}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status })
            });
        }
        await fetchJson(`/api/repair-jobs/${state.selectedJobId}/outcome`, {
            method: 'PUT',
            body: JSON.stringify({ repairOutcome: outcomeSelect.value })
        });
        await loadJobs();
        await loadDashboard();
        await renderJobDetail(state.selectedJobId);
    } catch (error) {
        alert(error.message);
    }
}

async function submitPaymentStatusForm(event) {
    event.preventDefault();
    if (!state.selectedJobId) {
        alert('Select a repair job first');
        return;
    }

    try {
        await fetchJson(`/api/repair-jobs/${state.selectedJobId}/payment-status`, {
            method: 'PUT',
            body: JSON.stringify({ paymentStatus: paymentStatusSelect.value })
        });
        await loadJobs();
        await renderJobDetail(state.selectedJobId);
    } catch (error) {
        alert(error.message);
    }
}

searchInput.addEventListener('input', async (event) => {
    const query = event.target.value.trim();
    try {
        const results = await fetchJson(`/api/repair-jobs/search?q=${encodeURIComponent(query)}`);
        renderJobs(results);
    } catch (error) {
        console.error(error);
    }
});

customerForm.addEventListener('submit', submitCustomerForm);
deviceForm.addEventListener('submit', submitDeviceForm);
jobForm.addEventListener('submit', submitJobForm);
repairUpdateForm.addEventListener('submit', submitRepairUpdateForm);
paymentStatusForm.addEventListener('submit', submitPaymentStatusForm);
customerList.addEventListener('click', handleInventoryAction);
deviceList.addEventListener('click', handleInventoryAction);

document.getElementById('jobCustomerSelect').addEventListener('change', async (event) => {
    const customerId = event.target.value;
    const deviceSelect = document.getElementById('jobDeviceSelect');
    if (!customerId) {
        deviceSelect.innerHTML = '<option value="">Select a customer first</option>';
        return;
    }
    const devices = await fetchJson('/api/devices');
    const filtered = devices.filter(device => device.customerId === Number(customerId));
    deviceSelect.innerHTML = '<option value="">Select a device</option>' + filtered
        .map(device => `<option value="${device.id}">${device.brand} ${device.model}</option>`)
        .join('');
});

function formatDate(value) {
    if (!value) return 'N/A';
    return new Date(value).toLocaleDateString();
}

function formatAmount(value) {
    return value == null ? 'Not set' : Number(value).toFixed(2);
}

function formatPaymentStatus(status) {
    return status === 'PAID' ? 'Paid' : 'Not paid';
}

function formatRepairStatus(status) {
    return {
        IN_PROGRESS: 'In progress',
        READY_FOR_COLLECTION: 'Ready for collection',
        COMPLETED: 'Completed'
    }[status] || status;
}

async function boot() {
    await checkHealth();
    try {
        const customers = await fetchJson('/api/customers');
        state.customers = customers;
        state.devices = await fetchJson('/api/devices');
        renderInventory();
        populateCustomerSelects();
        const jobData = await fetchJson('/api/repair-jobs');
        state.jobs = jobData;
        renderJobs(jobData);
        await loadDashboard();
    } catch (error) {
        console.error(error);
    }
}

boot();
