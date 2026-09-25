# System Settings UI

System Settings replaces the former Administration tab strip. Each sidebar item opens an independent page:

- Users: `#settings?page=users`
- Loan Categories: `#settings?page=loan-categories`
- I&E Categories: `#settings?page=ie-categories`
- Roles & Permissions: `#settings?page=roles`
- Audit Logs: `#settings?page=audit`

The pages retain administrator-only access and existing backend endpoints. Users and categories support add/edit and activation controls; users retain password reset. Built-in roles are read-only, and audit entries have a full detail view. No delete action is exposed because the existing APIs do not permit deletion.

All tables use the shared responsive DataTable with search, sorting, pagination, page-size selection, horizontal scrolling and loading/empty states. Each page fetches its own data. Existing `admin?tab=...` and loan-category bookmarks still resolve to the matching page. General configuration remains available through its existing bookmark or `#settings?page=general`.
