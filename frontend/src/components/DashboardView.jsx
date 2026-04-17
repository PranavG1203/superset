import { useEffect, useMemo, useState } from "react";
import { embedDashboard } from "@superset-ui/embedded-sdk";
import { fetchGuestToken } from "../services/api";

const supersetDomain =
  import.meta.env.VITE_SUPERSET_DOMAIN || "http://localhost:8088";

function DashboardView({ dashboard }) {
  const [embedError, setEmbedError] = useState("");

  const mountId = useMemo(
    () => `dashboard-${dashboard?.id || "empty"}`,
    [dashboard?.id],
  );

  useEffect(() => {
    if (!dashboard?.id) {
      setEmbedError("");
      return undefined;
    }

    if (!dashboard?.embeddedId) {
      setEmbedError(
        "This dashboard is missing an embedded ID. Open Superset dashboard embed settings and enable embedding.",
      );
      return undefined;
    }

    const mountPoint = document.getElementById(mountId);
    if (!mountPoint) {
      return undefined;
    }

    setEmbedError("");
    mountPoint.innerHTML = "";

    (async () => {
      try {
        await embedDashboard({
          id: String(dashboard.embeddedId),
          supersetDomain,
          mountPoint,
          fetchGuestToken: async () => {
            const token = await fetchGuestToken(
              dashboard.embeddedId || dashboard.uuid || dashboard.id,
            );
            return token;
          },
          dashboardUiConfig: {
            hideTitle: false,
            hideChartControls: false,
            filters: {
              expanded: true,
            },
          },
        });
      } catch (err) {
        const message =
          err?.response?.data?.message ||
          err?.message ||
          "Failed to embed dashboard.";
        setEmbedError(message);
      }
    })();

    return () => {
      if (mountPoint) {
        mountPoint.innerHTML = "";
      }
    };
  }, [dashboard, mountId]);

  return (
    <section className="viewer">
      <h2>
        {dashboard
          ? `Viewing: ${dashboard.title || dashboard.id}`
          : "Dashboard Viewer"}
      </h2>
      {!dashboard && <p>Select a dashboard to load it.</p>}
      {embedError && <p className="status error">{embedError}</p>}
      <div id={mountId} className="dashboard-mount" />
    </section>
  );
}

export default DashboardView;
