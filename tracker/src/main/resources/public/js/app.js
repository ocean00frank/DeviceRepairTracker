const state = {
    customers: [],
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
const statusForm = document.getElementById('statusForm');
const statusSelect = document.getElementById('statusSelect');
const costForm = document.getElementById('costForm');
const paymentForm = document.getElementById('paymentForm');
const outcomeForm = document.getElementById('outcomeForm');
const outcomeSelect = document.getElementById('outcomeSelect');

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
        tableBody.innerHTML = '<tr><td colspan="9"><div class="empty-state">No repair jobs found.</div></td></tr>';
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
    const allDevices = JSON.parse(sessionStorage.getItem('devices') || '[]');
    return allDevices.find(device => device.id === Number(deviceId)) || null;
}

async function renderJobDetail(jobId) {
    try {
        const result = await fetchJson(`/api/repair-jobs/${jobId}`);
        const { job, records, allowedNextStatuses, finance } = result;
        const device = getDeviceById(job.deviceId) || { deviceType: 'Device', brand: 'Unknown', model: 'Unknown' };
        const customer = state.customers.find(item => item.id === job.customerId) || { fullName: 'Unknown customer' };
        jobDetailEmpty.classList.add('hidden');
        jobDetailContent.classList.remove('hidden');

        document.getElementById('selectedJobContext').textContent = `Job #${job.id} · ${customer.fullName} · ${device.deviceType} ${device.brand} ${device.model}`;
        document.getElementById('detailFault').textContent = job.reportedFault;
        document.getElementById('detailOutcome').textContent = job.repairOutcome.replace(/_/g, ' ');
        outcomeSelect.value = job.repairOutcome;
        document.getElementById('repairCostDisplay').textContent = formatAmount(finance.repairCost);
        document.getElementById('totalPaidDisplay').textContent = formatAmount(finance.totalPaid);
        document.getElementById('balanceDisplay').textContent = formatAmount(finance.balance);

        const paymentList = document.getElementById('paymentList');
        paymentList.innerHTML = finance.payments.length
            ? finance.payments.map(payment => `<li>${formatAmount(payment.amount)} via ${payment.paymentMethod} - ${payment.paymentStatus} (${formatDate(payment.paidAt)})</li>`).join('')
            : '<li>No payments recorded yet.</li>';

        const statusLabels = {
            READY_FOR_COLLECTION: 'Ready for collection',
            COMPLETED: 'Completed'
        };
        statusSelect.innerHTML = '<option value="" selected disabled>Update status</option>' +
            allowedNextStatuses.map(status => `<option value="${status}">${statusLabels[status]}</option>`).join('');
        document.getElementById('saveStatusButton').disabled = allowedNextStatuses.length === 0;

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
        const devices = JSON.parse(sessionStorage.getItem('devices') || '[]');
        devices.push(device);
        sessionStorage.setItem('devices', JSON.stringify(devices));
        await loadCustomers();
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
                repairNotes: formData.get('repairNotes')
            })
        });
        jobForm.reset();
        await loadJobs();
        await loadDashboard();
    } catch (error) {
        alert(error.message);
    }
}

async function handleStatusUpdate(event) {
    event.preventDefault();
    if (!state.selectedJobId) {
        alert('Select a job first');
        return;
    }

    try {
        const status = statusSelect.value;
        await fetchJson(`/api/repair-jobs/${state.selectedJobId}/status`, {
            method: 'PUT',
            body: JSON.stringify({
                status
            })
        });
        await loadJobs();
        await loadDashboard();
    } catch (error) {
        alert(error.message);
    }
}

async function submitCostForm(event) {
    event.preventDefault();
    if (!state.selectedJobId) {
        alert('Select a job first');
        return;
    }

    const formData = new FormData(costForm);
    try {
        await fetchJson(`/api/repair-jobs/${state.selectedJobId}/cost`, {
            method: 'PUT',
            body: JSON.stringify({ repairCost: Number(formData.get('repairCost')) })
        });
        await loadJobs();
        await renderJobDetail(state.selectedJobId);
    } catch (error) {
        alert(error.message);
    }
}

async function submitOutcomeForm(event) {
    event.preventDefault();
    if (!state.selectedJobId) {
        alert('Select a job first');
        return;
    }

    try {
        await fetchJson(`/api/repair-jobs/${state.selectedJobId}/outcome`, {
            method: 'PUT',
            body: JSON.stringify({ repairOutcome: outcomeSelect.value })
        });
        await loadJobs();
        await renderJobDetail(state.selectedJobId);
    } catch (error) {
        alert(error.message);
    }
}

async function submitPaymentForm(event) {
    event.preventDefault();
    if (!state.selectedJobId) {
        alert('Select a job first');
        return;
    }

    const formData = new FormData(paymentForm);
    try {
        await fetchJson(`/api/repair-jobs/${state.selectedJobId}/payments`, {
            method: 'POST',
            body: JSON.stringify({
                amount: Number(formData.get('amount')),
                paymentMethod: formData.get('paymentMethod')
            })
        });
        paymentForm.reset();
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
statusForm.addEventListener('submit', handleStatusUpdate);
costForm.addEventListener('submit', submitCostForm);
paymentForm.addEventListener('submit', submitPaymentForm);
outcomeForm.addEventListener('submit', submitOutcomeForm);

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
        const deviceData = await fetchJson('/api/devices');
        sessionStorage.setItem('devices', JSON.stringify(deviceData));
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
