# Architecture

## Backend

The Spring Boot API follows a small reusable layered structure:

- Controllers validate HTTP input and enforce role access.
- Services contain financial rules and transactional workflows.
- Spring Data repositories isolate database access.
- JPA entities preserve member, workflow, schedule and audit history.
- API records keep persistence objects out of the JSON contract.
- Opaque bearer tokens are stored as SHA-256 hashes; passwords use BCrypt.

Financial records use a common workflow model with creator, submitter, approver, timestamps and decision remarks. There are no delete endpoints for financial transactions.

## Frontend

The React application uses:

- One reusable API client and authentication provider.
- A role-filtered two-level navigation model.
- Shared table, modal, form, badge, card and notification components.
- Feature pages for members, savings, loans, finance, approvals, reports and administration.
- Client-side CSV export for filtered reports.

## Database modes

- Default: persistent local H2 file database for a zero-configuration demonstration.
- Deployment: MySQL using the `mysql` Spring profile and environment variables.

For a real production rollout, add managed database backups, TLS, secrets management, external document storage, monitoring and organization-specific lending-policy tests before importing live financial data.

