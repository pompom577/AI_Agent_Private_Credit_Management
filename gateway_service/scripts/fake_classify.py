#!/usr/bin/env python3
"""
Tiny stand-in for the FastAPI /classify service used by Person 3.
Lets you manually exercise TC-GW-06 without standing up the real Python stack.

Usage:
    python3 scripts/fake_classify.py             # always responds 202
    python3 scripts/fake_classify.py --status 401  # simulate FastAPI auth fail
    python3 scripts/fake_classify.py --status 422  # simulate deal rejection

Every request is echoed to stdout (Authorization header + JSON body) so you
can paste the JWT into https://jwt.io to inspect the claims.
"""
from __future__ import annotations

import argparse
from http.server import BaseHTTPRequestHandler, HTTPServer


def make_handler(status_code: int) -> type[BaseHTTPRequestHandler]:
    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):  # noqa: N802 (stdlib naming)
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8", errors="replace")
            print("---- POST", self.path)
            print("Authorization:", self.headers.get("Authorization"))
            print("Body:", body)
            print()
            self.send_response(status_code)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"stubbed"}')

        def log_message(self, format, *args):  # silence default access log
            pass

    return Handler


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--port", type=int, default=8000)
    p.add_argument("--status", type=int, default=202,
                   help="HTTP status to return (default 202 Accepted)")
    args = p.parse_args()

    print(f"fake_classify listening on http://localhost:{args.port} -> returning {args.status}")
    HTTPServer(("", args.port), make_handler(args.status)).serve_forever()


if __name__ == "__main__":
    main()
