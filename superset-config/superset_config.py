# Copy this file into your Superset home (or point SUPERSET_CONFIG_PATH to it).
# Example on Windows PowerShell:
#   $env:SUPERSET_CONFIG_PATH="C:\Users\admin\Desktop\IBM\superset\superset-config\superset_config.py"

FEATURE_FLAGS = {
    "EMBEDDED_SUPERSET": True,
}

# Required for Superset startup; replace with your own strong random key for production.
SECRET_KEY = "3fd7f4f2c2a24b63a71983e7f671f72a1d9f2a5c8b6e4f1d9a3b7c5e1f9d4a2"

# Enable backend-only guest token generation without browser CSRF session cookies.
WTF_CSRF_EXEMPT_LIST = [
    "superset.security.api.guest_token",
]

# Allow the React app to call Superset APIs when needed.
ENABLE_CORS = True
CORS_OPTIONS = {
    "supports_credentials": True,
    "allow_headers": ["*"],
    "resources": [r"/*"],
    "origins": [
        "http://localhost:5173",
        "http://localhost:8080",
    ],
}

# Superset 6.x embedding works best with explicit cookie semantics.
# For local HTTP development, keep secure=False; set True behind HTTPS in production.
SESSION_COOKIE_SAMESITE = "None"
SESSION_COOKIE_SECURE = False

# If you run with strict clickjacking protection, allow your frontend host.
TALISMAN_ENABLED = True
TALISMAN_CONFIG = {
    "content_security_policy": {
        "default-src": ["'self'"],
        "img-src": ["'self'", "data:", "blob:", "https:"],
        "worker-src": ["'self'", "blob:"],
        "connect-src": ["'self'", "http://localhost:8088", "http://localhost:8080", "http://localhost:5173"],
        "style-src": ["'self'", "'unsafe-inline'"],
        "frame-ancestors": ["'self'", "http://localhost:5173"],
        "frame-src": ["'self'", "http://localhost:8088"],
    },
    "force_https": False,
}

# Local dev shortcut: grant guest tokens full access so embedded dashboards can load.
# IMPORTANT: change this to a least-privilege custom role in production.
GUEST_ROLE_NAME = "Admin"
