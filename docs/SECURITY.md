# Security Model

This document outlines how authentication and authorization works across the platform.

---

## 1. User Authentication

Options:
- Local JWT
- External provider (OIDC)

Roles:
- Admin
- Operator
- Viewer

---

## 2. Node Authentication

Two supported models:

### Option A: Token-based
- Each Node has a static token
- Sent via header on every request

### Option B: mTLS
- Client certificate per Node
- Most secure option

---

## 3. Authorization

Rules:
- Admin: full access
- Operator: manage instances, view nodes/templates
- Viewer: read-only
- Node: only allowed to access its callback endpoints

---

## 4. API Separation

- `/api/brain/...` → Admin & Operators
- `/api/node/...` → Node callbacks
- `/api/public/...` → Optional public info
