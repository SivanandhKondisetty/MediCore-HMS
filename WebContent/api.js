/**
 * api.js — Frontend ↔ Java Servlet Bridge
 * All fetch() calls go to Tomcat at http://localhost:8080/hospital/api/
 * Project ID: 25FE5A4305
 */

const BASE = "http://localhost:8080/hospital/api";

// ─── Generic fetch helpers ────────────────────────────────────
async function apiGet(endpoint) {
    const res = await fetch(BASE + endpoint);
    return res.json();
}

async function apiPost(endpoint, params) {
    const body = new URLSearchParams(params);
    const res = await fetch(BASE + endpoint, { method: "POST", body });
    return res.json();
}

async function apiPut(endpoint, params) {
    const body = new URLSearchParams(params);
    const res = await fetch(BASE + endpoint, { method: "PUT", body });
    return res.json();
}

async function apiDelete(endpoint) {
    const res = await fetch(BASE + endpoint, { method: "DELETE" });
    return res.json();
}

// ─── AUTH ──────────────────────────────────────────────────────
const API = {

    login: (email, password) =>
        apiPost("/auth?action=login", { email, password }),

    signup: (name, specialization, department_id, email, password) =>
        apiPost("/auth?action=signup", { name, specialization, department_id, email, password }),

    // ─── PATIENTS ──────────────────────────────────────────────
    getPatients: () =>
        apiGet("/patients"),

    addPatient: (name, dob, contact, address) =>
        apiPost("/patients", { name, dob, contact, address }),

    updatePatient: (id, contact, address) =>
        apiPut(`/patients?id=${id}`, { contact, address }),

    deletePatient: (id) =>
        apiDelete(`/patients?id=${id}`),

    getPatientHistory: (id) =>
        apiGet(`/patients?action=history&id=${id}`),

    // ─── DOCTORS ───────────────────────────────────────────────
    getDoctors: () =>
        apiGet("/doctors"),

    addDoctor: (name, specialization, department_id, email, password) =>
        apiPost("/doctors", { name, specialization, department_id, email, password }),

    deleteDoctor: (id) =>
        apiDelete(`/doctors?id=${id}`),

    // ─── APPOINTMENTS ──────────────────────────────────────────
    getAppointments: () =>
        apiGet("/appointments"),

    getMyAppointments: (doctor_id) =>
        apiGet(`/appointments?doctor_id=${doctor_id}`),

    scheduleAppointment: (patient_id, doctor_id, appointment_date) =>
        apiPost("/appointments", { patient_id, doctor_id, appointment_date }),

    updateAppointmentStatus: (id, status) =>
        apiPut(`/appointments?id=${id}`, { status }),

    deleteAppointment: (id) =>
        apiDelete(`/appointments?id=${id}`),

    // ─── BILLING ───────────────────────────────────────────────
    getBilling: () =>
        apiGet("/billing"),

    addBill: (patient_id, amount, billing_date) =>
        apiPost("/billing", { patient_id, amount, billing_date }),

    deleteBill: (id) =>
        apiDelete(`/billing?id=${id}`),

    // ─── DEPARTMENTS ───────────────────────────────────────────
    getDepartments: () =>
        apiGet("/departments"),

    addDepartment: (department_name) =>
        apiPost("/departments", { department_name }),

    deleteDepartment: (id) =>
        apiDelete(`/departments?id=${id}`),

    // ─── REPORTS ───────────────────────────────────────────────
    getReport: (type) =>
        apiGet(`/reports?type=${type}`),

    // ─── SQL CONSOLE ───────────────────────────────────────────
    runQuery: (query) =>
        apiPost("/sql", { query })
};
