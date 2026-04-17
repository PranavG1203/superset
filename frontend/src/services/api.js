import axios from 'axios';

const backendBaseUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: backendBaseUrl,
  timeout: 20000,
});

export async function fetchDashboards() {
  const response = await api.get('/dashboards');
  return response.data;
}

export async function fetchGuestToken(dashboardId) {
  const response = await api.post('/get-guest-token', { dashboardId: String(dashboardId) });
  return response.data.token;
}
