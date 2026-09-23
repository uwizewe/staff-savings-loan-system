# Project structure

The application uses feature-based packages. Existing API paths, browser hash routes,
database table names, role rules, and financial workflows are preserved.

## Backend

Source root: `backend/src/main/java/com/binava/stafffinance/`.
`StaffFinanceApplication.java` remains at the root so Spring discovers all feature packages.
Each class, record, enum, and repository interface lives in its own matching Java file.

| Package | Responsibility |
| --- | --- |
| `auth/` | Login/logout controllers and DTOs, authentication service, token repository/filter |
| `member/` | Member register and statements; controller, DTO, entity, mapper, repository, service |
| `savings/` | Individual savings, withdrawals, monthly batches and their approvals |
| `loan/` | Applications, approval terms, disbursement, schedules, calculation and mappings |
| `repayment/` | Individual and batch repayments, allocation and loan completion |
| `approval/` | Approval queue and decisions, shared workflow state and rules |
| `finance/` | Income/expenses and finance category administration |
| `dashboard/` | Dashboard controller, view DTO and aggregation service |
| `user/` | User account administration, DTOs, entity, mapper and repository |
| `role/` | Role enum |
| `permission/` | Role permission catalogue controller and service |
| `audit/` | Audit entity/repository, transaction logging and administrative queries |
| `security/` | Spring Security filter-chain and CORS configuration |
| `config/` | Demo seeding, system setting entities, queries and administration |
| `common/` | Shared base entity, money/reference helpers and batch DTO |
| `exception/` | API error DTO, exceptions and global exception handling |

Controllers accept validated DTOs and delegate to transactional services. Repositories own
persistence queries and fetch plans; feature mappers produce API view DTOs. The existing
field-access JPA model is retained to keep this a structural refactor. Entity and table names
have not changed, so no database migration is required.

Repayment URLs remain under `/api/loans/repayments` and `/api/loans/repayment-batches`,
but are now implemented by `repayment/controller/RepaymentController.java` and
`repayment/service/RepaymentService.java`. The loan service handles the loan lifecycle only.

Administration URLs remain under `/api/admin`: users are handled by `UserController`,
categories by `FinanceCategoryController`, settings by `SystemSettingController`,
permissions by `PermissionController`, and audit history by `AuditController`.

`guarantor/`, `notification/`, backend `reports/`, and unused layer directories contain
`package-info.java` markers. These are reserved extension points, not implemented APIs.
Reports currently run in the frontend against existing data endpoints; approvals use the
shared workflow entities rather than a separate approval table. No new business feature
has been invented as part of the folder change.

Tests live under the same base package in `backend/src/test/java/`: `integration/` contains
HTTP/workflow tests and `loan/service/` contains calculation and service regression tests.

## Frontend

Source root: `frontend/src/`.

```text
src/
  app/                 App.jsx, router.jsx, providers.jsx
  assets/              images/, icons/, logos/
  components/
    ui/                shared UI primitives and modal
    layout/            application shell and account dropdown
    common/            reserved cross-feature components
  features/
    auth/              login page, auth service, useAuth hook
    dashboard/         dashboard page
    members/           member register and statement pages
    savings/           savings page and savings table
    loans/             loan page, loan table and schedule modal
    repayments/        repayment table (used by the loan workspace)
    approvals/         approval page and reusable approval actions
    finance/           finance page
    reports/           report page, calculation/filter service and tests
    users/             administration workspace
    guarantors/        reserved
    roles/             reserved standalone role UI
    settings/          reserved standalone settings UI
  services/            api.js (shared authenticated fetch client)
  hooks/               useApiData.js
  context/             AuthContext.jsx
  utils/               formatting, CSV download and route utilities
  constants/           navigation.js
  styles/              index.css
  main.jsx             React entry point
```

Existing combined workspaces remain intact: loan and repayment tabs share the loan page;
users, roles, categories, settings, and audit tabs share the administration page.
Empty feature layers are tracked with `.gitkeep` until they have an implementation.

The app already uses native `fetch`, so `services/api.js` remains the HTTP client.
There is no Axios dependency or misleading `axiosInstance.js` wrapper. Add a separate
transport module only if the application actually adopts Axios.

## Development and verification

Run the backend from `backend/` to preserve the relative H2 data-file location:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Run the frontend from `frontend/`:

```powershell
npm run dev
npm run build
npm test
npm run check:structure
```

The structural check verifies Java package/file names, application imports, and relative
frontend imports. A clean Java build is required after package moves so old compiled
controllers/entities do not remain on the classpath. Restart both development servers
after this migration. No live database records are changed by the test suite.

For a new feature, put HTTP boundaries in `controller`, contracts in `dto`, persistence in
`entity` and `repository`, and use cases in `service`. Keep cross-feature helpers in
`common` only when they are genuinely shared. Add pages and feature-specific components
under `features/<feature>`; shared presentation primitives belong in `components/ui`.
