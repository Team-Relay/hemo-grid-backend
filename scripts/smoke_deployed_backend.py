#!/usr/bin/env python3
"""Read-only HemoGrid deployed API smoke checks; no request or inventory mutations."""
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = os.getenv("HEMOGRID_API_BASE", "https://hemo-grid-backend.onrender.com").rstrip("/")
TIMEOUT = float(os.getenv("HEMOGRID_SMOKE_TIMEOUT", "30"))


def request(method, path, body=None, bearer=None, origin=None):
    url = BASE + path
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if bearer:
        headers["Authorization"] = "Bearer " + bearer
    if origin:
        headers["Origin"] = origin
        headers["Access-Control-Request-Method"] = "GET"
        headers["Access-Control-Request-Headers"] = "authorization"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, headers=headers, data=data, method=method)
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as res:
            return res.status, dict(res.headers), res.read()
    except urllib.error.HTTPError as exc:
        return exc.code, dict(exc.headers), exc.read()


def check_status(label, method, path, expected, **kw):
    status, headers, data = request(method, path, **kw)
    if status != expected:
        raise AssertionError(f"{label}: expected HTTP {expected}, got HTTP {status}")
    print(f"PASS {label}: HTTP {status}")
    return headers, data


def main():
    parsed = urllib.parse.urlsplit(BASE)
    if parsed.scheme not in ("https", "http") or not parsed.netloc or parsed.username:
        raise ValueError("HEMOGRID_API_BASE must be a valid HTTP(S) URL without credentials")
    print(f"Checking: {BASE} (read-only smoke test)")
    _, data = check_status("health", "GET", "/actuator/health", 200)
    health = json.loads(data)
    if health.get("status") != "UP":
        raise AssertionError("Health status is not UP")
    _, data = check_status("readiness", "GET", "/actuator/health/readiness", 200)
    if json.loads(data).get("status") != "UP":
        raise AssertionError("Readiness status is not UP")
    _, data = check_status("OpenAPI", "GET", "/v3/api-docs", 200)
    openapi = json.loads(data)
    expected_paths = (
        "/api/v1/auth/login",
        "/api/v1/blood-requests",
        "/api/v1/dispatch-plans/requests",
        "/api/v1/dispatch-plans/preview",
        "/api/v1/provider/requests",
    )
    missing = [p for p in expected_paths if p not in openapi.get("paths", {})]
    if missing:
        raise AssertionError("OpenAPI is missing endpoint(s): " + ", ".join(missing))
    print("PASS expected API paths present")

    check_status("unauthenticated request denied", "GET", "/api/v1/blood-requests", 401)
    origin = os.getenv("HEMOGRID_SMOKE_ORIGIN")
    if origin:
        headers, _ = check_status("frontend CORS preflight", "OPTIONS",
                                  "/api/v1/dispatch-plans/requests", 200, origin=origin)
        # A strict origin match must be sent back when the host authorizes this frontend.
        allowed = next((v for k, v in headers.items() if k.lower() == "access-control-allow-origin"), None)
        if allowed != origin:
            raise AssertionError(f"CORS expected origin {origin!r}, returned {allowed!r}")
        print("PASS CORS allowed origin matches requested frontend")

    email = os.getenv("HEMOGRID_SMOKE_EMAIL")
    password = os.getenv("HEMOGRID_SMOKE_PASSWORD")
    if bool(email) != bool(password):
        raise ValueError("Set both HEMOGRID_SMOKE_EMAIL and HEMOGRID_SMOKE_PASSWORD, or neither")
    if email:
        _, data = check_status("demo login", "POST", "/api/v1/auth/login", 200,
                               body={"email": email, "password": password})
        payload = json.loads(data)
        token = payload.get("accessToken")
        if not token or payload.get("tokenType") != "Bearer":
            raise AssertionError("Login did not return a bearer access token")
        check_status("authenticated identity", "GET", "/api/v1/auth/me", 200, bearer=token)
        role = (payload.get("user") or {}).get("role")
        if role == "PLATFORM_ADMIN":
            check_status("coordinator pending requests", "GET", "/api/v1/dispatch-plans/requests",
                         200, bearer=token)
        elif role in ("HOSPITAL_ADMIN", "HOSPITAL_STAFF"):
            check_status("hospital request listing", "GET", "/api/v1/blood-requests",
                         200, bearer=token)
        elif role in ("BLOOD_BANK_ADMIN", "BLOOD_BANK_STAFF"):
            check_status("bank request listing", "GET", "/api/v1/provider/requests",
                         200, bearer=token)
        else:
            raise AssertionError(f"Unexpected synthetic demo user role: {role!r}")
    print("PASS read-only deployed smoke test completed. No mutations performed.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, AssertionError, json.JSONDecodeError) as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        sys.exit(1)
