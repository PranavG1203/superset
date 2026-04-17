function DashboardList({ dashboards, selectedDashboardEmbeddedId, onSelect }) {
  if (!dashboards.length) {
    return (
      <aside className="sidebar">
        <h2>Dashboards</h2>
        <p>No dashboards were found.</p>
      </aside>
    );
  }

  return (
    <aside className="sidebar">
      <h2>Dashboards</h2>
      <div className="cards">
        {dashboards.map((dashboard) => (
          <button
            key={dashboard.id}
            type="button"
            className={`dashboard-card ${selectedDashboardEmbeddedId === dashboard.embeddedId ? "active" : ""}`}
            onClick={() => onSelect(dashboard)}
          >
            <h3>{dashboard.title || `Dashboard ${dashboard.id}`}</h3>
            <p>ID: {dashboard.id}</p>
            {dashboard.embeddedId && <p>Embed ID: {dashboard.embeddedId}</p>}
            {dashboard.slug && <p>Slug: {dashboard.slug}</p>}
          </button>
        ))}
      </div>
    </aside>
  );
}

export default DashboardList;
