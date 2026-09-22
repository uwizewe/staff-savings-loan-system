# API Quick Reference

Base URL: `http://localhost:8081/api`

All routes except login require `Authorization: Bearer <token>`.

## Authentication

| Method | Route | Purpose |
| --- | --- | --- |
| POST | `/auth/login` | Sign in and receive an opaque bearer token |
| GET | `/auth/me` | Get the signed-in user |
| POST | `/auth/logout` | Revoke the current token |

## Members

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/members` | Search/filter members |
| POST | `/members` | Register a member |
| PUT | `/members/{id}` | Update a member without deleting history |
| GET | `/members/{id}/statement` | Consolidated savings and loan statement |

## Savings

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/savings` | Savings history; supports `memberId`, `type`, `status` |
| POST | `/savings/individual` | Create individual savings draft |
| POST | `/savings/withdrawals` | Create withdrawal draft |
| POST | `/savings/{id}/submit` | Submit a draft |
| POST | `/savings/{id}/decision` | Approve or reject |
| GET/POST | `/savings/batches` | List/create monthly savings batches |
| POST | `/savings/batches/{id}/submit` | Submit a monthly batch |
| POST | `/savings/batches/{id}/decision` | Approve or reject a monthly batch |

## Loans and repayments

| Method | Route | Purpose |
| --- | --- | --- |
| GET/POST | `/loans` | List/create loan applications |
| POST | `/loans/{id}/submit` | Submit loan application |
| POST | `/loans/{id}/approve` | Approve with final terms |
| POST | `/loans/{id}/reject` | Reject application |
| POST | `/loans/{id}/disburse` | Activate loan and generate schedule |
| GET | `/loans/{id}/schedule` | View repayment schedule |
| GET/POST | `/loans/repayments` | List/create individual repayments |
| POST | `/loans/repayments/{id}/submit` | Submit repayment |
| POST | `/loans/repayments/{id}/decision` | Approve or reject repayment |
| GET/POST | `/loans/repayment-batches` | List/create monthly repayment batches |

## Income, expenses and approvals

| Method | Route | Purpose |
| --- | --- | --- |
| GET/POST | `/finance` | List/create income or expense transactions |
| POST | `/finance/{id}/submit` | Submit transaction |
| POST | `/finance/{id}/decision` | Approve or reject transaction |
| GET | `/approvals` | Combined pending approval work queue |
| POST | `/approvals/{type}/{id}/decision` | Decide from the unified queue |

## Management and administration

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/dashboard` | Permission-aware dashboard metrics |
| GET/POST/PUT | `/admin/users` | User administration |
| GET | `/admin/permissions` | Role responsibility matrix |
| GET/POST/PUT | `/admin/categories` | Income and expense categories |
| GET/PUT | `/admin/settings` | Savings and lending settings |
| GET | `/admin/audit-logs` | Latest 250 audited actions |

Interactive OpenAPI documentation is available at `/swagger-ui.html` while the backend is running.

