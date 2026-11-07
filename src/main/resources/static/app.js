 // University Management System - Frontend Vanilla JavaScript

// Application State
let authToken = null;
let currentUserEmail = null;
let currentUserRole = null;
let currentAccountId = null;
let currentResource = null; // 'admins', 'teachers', or 'students'
let editingAccountId = null; // null when creating, or account ID when editing

// DOM Elements
const loginSection = document.getElementById("login-section");
const dashboardSection = document.getElementById("dashboard-section");
const loginForm = document.getElementById("login-form");
const alertBox = document.getElementById("alert-box");

const userEmailDisplay = document.getElementById("user-email-display");
const userRoleBadge = document.getElementById("user-role-badge");
const btnLogout = document.getElementById("btn-logout");

const tabAdmins = document.getElementById("tab-admins");
const tabTeachers = document.getElementById("tab-teachers");
const tabStudents = document.getElementById("tab-students");

const resourceTitle = document.getElementById("resource-title");
const btnShowCreateForm = document.getElementById("btn-show-create-form");
const accountFormContainer = document.getElementById("account-form-container");
const accountForm = document.getElementById("account-form");
const formHeading = document.getElementById("form-heading");
const accountIdInput = document.getElementById("account-id");
const accountNameInput = document.getElementById("account-name");
const accountEmailInput = document.getElementById("account-email");
const accountPasswordInput = document.getElementById("account-password");
const btnCancelForm = document.getElementById("btn-cancel-form");
const recordsTableBody = document.getElementById("records-table-body");

// Initialize Event Listeners
document.addEventListener("DOMContentLoaded", () => {
    loginForm.addEventListener("submit", handleLogin);
    btnLogout.addEventListener("click", handleLogout);

    tabAdmins.addEventListener("click", () => switchResourceTab("admins"));
    tabTeachers.addEventListener("click", () => switchResourceTab("teachers"));
    tabStudents.addEventListener("click", () => switchResourceTab("students"));

    btnShowCreateForm.addEventListener("click", showCreateForm);
    btnCancelForm.addEventListener("click", hideAccountForm);
    accountForm.addEventListener("submit", handleSaveAccount);
});

// Show alert message (success or error)
function showAlert(message, isError = false) {
    alertBox.textContent = message;
    alertBox.className = "alert-box " + (isError ? "alert-error" : "alert-success");
    alertBox.style.display = "block";
    window.scrollTo({ top: 0, behavior: "smooth" });

    setTimeout(() => {
        alertBox.style.display = "none";
    }, 5000);
}

function hideAlert() {
    alertBox.style.display = "none";
}

// 1. LOGIN
async function handleLogin(event) {
    event.preventDefault();
    hideAlert();

    const email = document.getElementById("login-email").value.trim();
    const password = document.getElementById("login-password").value;

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email: email, password: password })
        });

        const data = await response.json();

        if (!response.ok) {
            showAlert(data.error || "Login failed", true);
            return;
        }

        // Store authenticated session state
        authToken = data.accessToken;
        currentUserRole = data.role;
        currentUserEmail = data.email || email;
        currentAccountId = data.accountId || null;

        // Clear login password input
        document.getElementById("login-password").value = "";

        showAlert("Login successful! Welcome, " + currentUserEmail, false);
        showDashboard();

    } catch (err) {
        showAlert("Cannot connect to server. Please check if the backend is running.", true);
    }
}

// 2. LOGOUT
async function handleLogout() {
    hideAlert();

    if (authToken) {
        try {
            await fetch("/api/auth/logout", {
                method: "POST",
                headers: {
                    "Authorization": "Bearer " + authToken
                }
            });
        } catch (err) {
            console.error("Logout request error", err);
        }
    }

    // Reset local state
    authToken = null;
    currentUserEmail = null;
    currentUserRole = null;
    currentAccountId = null;
    currentResource = null;
    editingAccountId = null;

    showLoginView();
    showAlert("Logged out successfully.", false);
}

// Switch view to Dashboard
function showDashboard() {
    loginSection.style.display = "none";
    dashboardSection.style.display = "block";

    userEmailDisplay.textContent = currentUserEmail;
    userRoleBadge.textContent = currentUserRole;

    // Show only the tabs permitted for this role
    tabAdmins.style.display = (currentUserRole === "ADMIN") ? "inline-block" : "none";
    tabTeachers.style.display = (currentUserRole === "ADMIN" || currentUserRole === "TEACHER") ? "inline-block" : "none";
    tabStudents.style.display = (currentUserRole === "ADMIN" || currentUserRole === "STUDENT") ? "inline-block" : "none";

    // Select default tab
    if (currentUserRole === "ADMIN") {
        switchResourceTab("teachers");
    } else if (currentUserRole === "TEACHER") {
        switchResourceTab("teachers");
    } else {
        switchResourceTab("students");
    }
}

// Switch view to Login
function showLoginView() {
    dashboardSection.style.display = "none";
    loginSection.style.display = "block";
    hideAccountForm();
}

// Switch between Admins, Teachers, and Students tabs
function switchResourceTab(resource) {
    currentResource = resource;
    hideAccountForm();

    // Update active tab button style
    [tabAdmins, tabTeachers, tabStudents].forEach(tab => {
        if (tab.dataset.resource === resource) {
            tab.classList.add("active");
        } else {
            tab.classList.remove("active");
        }
    });

    const capitalizedName = resource.charAt(0).toUpperCase() + resource.slice(1);
    resourceTitle.textContent = "Manage " + capitalizedName;
    btnShowCreateForm.textContent = "+ Add New " + capitalizedName.slice(0, -1);

    // Only ADMIN can create accounts
    btnShowCreateForm.style.display = (currentUserRole === "ADMIN") ? "inline-block" : "none";

    loadRecords();
}

// 3. READ: Fetch and display records in table
async function loadRecords() {
    recordsTableBody.innerHTML = '<tr><td colspan="5" class="text-center">Loading...</td></tr>';

    try {
        const response = await fetch("/api/" + currentResource, {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + authToken
            }
        });

        if (response.status === 401) {
            showAlert("Session expired. Please log in again.", true);
            handleLogout();
            return;
        }

        if (response.status === 403) {
            showAlert("You do not have permission to view " + currentResource, true);
            recordsTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Access Denied</td></tr>';
            return;
        }

        const accounts = await response.json();

        if (!Array.isArray(accounts) || accounts.length === 0) {
            recordsTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No records found.</td></tr>';
            return;
        }

        recordsTableBody.innerHTML = "";
        accounts.forEach(account => {
            const tr = document.createElement("tr");

            const tdId = document.createElement("td");
            tdId.textContent = account.id;

            const tdName = document.createElement("td");
            tdName.textContent = account.fullName;

            const tdEmail = document.createElement("td");
            tdEmail.textContent = account.email;

            const tdRole = document.createElement("td");
            const roleBadge = document.createElement("span");
            roleBadge.className = "badge";
            roleBadge.textContent = account.role;
            tdRole.appendChild(roleBadge);

            const tdActions = document.createElement("td");

            // Check if this row is the logged-in user's own account
            const isOwnRecord = (currentAccountId && account.id === currentAccountId) ||
                                (currentUserEmail && account.email.toLowerCase() === currentUserEmail.toLowerCase());

            // ADMIN can edit all accounts; TEACHER and STUDENT can edit their own profile
            const canEdit = (currentUserRole === "ADMIN" || isOwnRecord);

            // Only ADMIN can delete accounts
            const canDelete = (currentUserRole === "ADMIN");

            if (canEdit || canDelete) {
                if (canEdit) {
                    const editBtn = document.createElement("button");
                    editBtn.className = "btn btn-secondary btn-sm";
                    editBtn.textContent = "Edit";
                    editBtn.addEventListener("click", () => showEditForm(account));
                    tdActions.appendChild(editBtn);
                }

                if (canDelete) {
                    const deleteBtn = document.createElement("button");
                    deleteBtn.className = "btn btn-danger btn-sm";
                    deleteBtn.textContent = "Delete";
                    deleteBtn.addEventListener("click", () => handleDeleteAccount(account.id, account.fullName));
                    tdActions.appendChild(deleteBtn);
                }
            } else {
                tdActions.textContent = "-";
            }

            tr.appendChild(tdId);
            tr.appendChild(tdName);
            tr.appendChild(tdEmail);
            tr.appendChild(tdRole);
            tr.appendChild(tdActions);

            recordsTableBody.appendChild(tr);
        });

    } catch (err) {
        recordsTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Failed to load records.</td></tr>';
        showAlert("Error loading records: " + err.message, true);
    }
}

// Show Create Form
function showCreateForm() {
    editingAccountId = null;
    formHeading.textContent = "Create New " + currentResource.slice(0, -1);
    accountIdInput.value = "";
    accountNameInput.value = "";
    accountEmailInput.value = "";
    accountPasswordInput.value = "";
    accountPasswordInput.required = true;
    accountFormContainer.style.display = "block";
    accountNameInput.focus();
}

// Show Edit Form
function showEditForm(account) {
    editingAccountId = account.id;
    formHeading.textContent = "Edit " + currentResource.slice(0, -1) + " #" + account.id;
    accountIdInput.value = account.id;
    accountNameInput.value = account.fullName;
    accountEmailInput.value = account.email;
    accountPasswordInput.value = "";
    accountPasswordInput.placeholder = "Enter new password (8 to 72 chars)";
    accountPasswordInput.required = true;
    accountFormContainer.style.display = "block";
    accountNameInput.focus();
}

// Hide Form
function hideAccountForm() {
    accountFormContainer.style.display = "none";
    editingAccountId = null;
    accountForm.reset();
}

// 4. CREATE / UPDATE
async function handleSaveAccount(event) {
    event.preventDefault();
    hideAlert();

    const fullName = accountNameInput.value.trim();
    const email = accountEmailInput.value.trim();
    const password = accountPasswordInput.value;

    const payload = {
        fullName: fullName,
        email: email,
        password: password
    };

    const isEdit = (editingAccountId !== null);
    const url = isEdit ? ("/api/" + currentResource + "/" + editingAccountId) : ("/api/" + currentResource);
    const method = isEdit ? "PUT" : "POST";

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + authToken
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (!response.ok) {
            showAlert(data.error || "Operation failed", true);
            return;
        }

        // If the logged-in user updated their own account, update email state and banner
        if (editingAccountId === currentAccountId || (currentUserEmail && data.email && editingAccountId === data.id)) {
            currentUserEmail = data.email;
            userEmailDisplay.textContent = currentUserEmail;
        }

        showAlert((isEdit ? "Updated" : "Created") + " account successfully: " + data.fullName, false);
        hideAccountForm();
        loadRecords();

    } catch (err) {
        showAlert("Error saving account: " + err.message, true);
    }
}

// 5. DELETE
async function handleDeleteAccount(id, fullName) {
    const confirmed = confirm("Are you sure you want to delete the account for '" + fullName + "' (ID: " + id + ")?");
    if (!confirmed) {
        return;
    }

    hideAlert();

    try {
        const response = await fetch("/api/" + currentResource + "/" + id, {
            method: "DELETE",
            headers: {
                "Authorization": "Bearer " + authToken
            }
        });

        if (response.status === 204) {
            showAlert("Account deleted successfully.", false);
            loadRecords();
            return;
        }

        const data = await response.json();
        showAlert(data.error || "Failed to delete account", true);

    } catch (err) {
        showAlert("Error deleting account: " + err.message, true);
    }
}
