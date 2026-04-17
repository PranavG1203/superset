import { useEffect, useState } from "react";
import DashboardList from "./components/DashboardList";
import DashboardView from "./components/DashboardView";
import { fetchDashboards } from "./services/api";

function App() {
  const [dashboards, setDashboards] = useState([]);
  const [selectedDashboard, setSelectedDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDashboards = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await fetchDashboards();
        setDashboards(data);
      } catch (err) {
        const message =
          err?.response?.data?.message ||
          err.message ||
          "Failed to load dashboards.";
        setError(message);
      } finally {
        setLoading(false);
      }
    };

    loadDashboards();
  }, []);

  return (
    <div className="app-shell">
      <header>
        <h1>Superset Dashboard Portal</h1>
        <p>
          Pick a dashboard to securely embed it with a backend-generated guest
          token.
        </p>
      </header>

      {loading && <p className="status">Loading dashboards...</p>}
      {error && <p className="status error">{error}</p>}

      {!loading && !error && (
        <div className="layout">
          <DashboardList
            dashboards={dashboards}
            selectedDashboardEmbeddedId={selectedDashboard?.embeddedId}
            onSelect={setSelectedDashboard}
          />
          <DashboardView dashboard={selectedDashboard} />
        </div>
      )}
    </div>
  );
}

export default App;
