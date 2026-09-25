import UsersPage from '../../users/pages/UsersPage.jsx';
import LoanCategoriesPage from './LoanCategoriesPage.jsx';
import FinanceCategoriesPage from './FinanceCategoriesPage.jsx';
import RolesPermissionsPage from './RolesPermissionsPage.jsx';
import AuditLogsPage from './AuditLogsPage.jsx';
import GeneralSettingsPage from './GeneralSettingsPage.jsx';
import { Card } from '../../../components/ui/index.jsx';

const pages = { users: UsersPage, 'loan-categories': LoanCategoriesPage, 'ie-categories': FinanceCategoriesPage, roles: RolesPermissionsPage, audit: AuditLogsPage, general: GeneralSettingsPage };
const aliases = { categories: 'ie-categories', settings: 'general' };

export default function SystemSettingsPage({ route }) {
  const params = new URLSearchParams(route.split('?')[1]);
  const requested = params.get('page') || params.get('tab') || 'users';
  const name = aliases[requested] || requested;
  const Page = pages[name];
  return Page ? <Page key={name}/> : <Card>Setting page not found. Choose a page from System Settings.</Card>;
}
