import {
  BarChart3, BookOpenCheck, ChevronDown, CircleDollarSign, ClipboardCheck,
  Coins, FileBarChart, Gauge, Landmark, LogOut, Menu, Settings, ShieldCheck,
  Users, UserRound, WalletCards, X,
} from "lucide-react";

export const menu = [
  { label: "Dashboard", icon: Gauge, route: "dashboard", roles: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"] },
  {
    label: "Members", icon: Users, roles: ["INITIATOR", "APPROVER", "ADMIN"], children: [
      { label: "All members", route: "members" },
      { label: "Add member", route: "members?view=add", roles: ["INITIATOR", "ADMIN"] },
      { label: "Member statements", route: "members?view=statements" },
    ],
  },
  {
    label: "Savings", icon: Coins, roles: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"], children: [
      { label: "Savings history", route: "savings?tab=history" },
      { label: "Monthly savings", route: "savings?tab=monthly", roles: ["INITIATOR", "APPROVER", "ADMIN"] },
      { label: "Individual savings", route: "savings?tab=individual", roles: ["INITIATOR", "ADMIN"] },
      { label: "Withdrawals", route: "savings?tab=withdrawal", roles: ["INITIATOR", "ADMIN"] },
    ],
  },
  {
    label: "Loans", icon: Landmark, roles: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"], children: [
      { label: "Loan applications", route: "loans?view=applications" },
      { label: "Active loans", route: "loans?view=active" },
      { label: "Loan repayments", route: "loans?view=repayments" },
      { label: "Monthly repayments", route: "loans?view=monthly", roles: ["INITIATOR", "APPROVER", "ADMIN"] },
    ],
  },
  {
    label: "Income & Expenses", icon: WalletCards, roles: ["INITIATOR", "APPROVER", "ADMIN"], children: [
      { label: "All transactions", route: "finance?tab=history" },
      { label: "Income", route: "finance?tab=income" },
      { label: "Expenses", route: "finance?tab=expense" },
    ],
  },
  { label: "Approvals", icon: ClipboardCheck, route: "approvals", roles: ["APPROVER", "ADMIN"] },
  { label: "Reports", icon: FileBarChart, route: "reports", roles: ["INITIATOR", "APPROVER", "ADMIN"] },
  {
    label: "Administration", icon: Settings, roles: ["ADMIN"], children: [
      { label: "Users", route: "admin?tab=users" },
      { label: "Roles & permissions", route: "admin?tab=roles" },
      { label: "Categories", route: "admin?tab=categories" },
      { label: "Audit logs", route: "admin?tab=audit" },
    ],
  },
  { label: "System Settings", icon: Settings, roles: ["ADMIN"], children: [
    { label: "Loan Categories", route: "settings?tab=loan-categories" },
    { label: "General settings", route: "admin?tab=settings" },
  ] },
  { label: "My statement", icon: BookOpenCheck, route: "statement", roles: ["MEMBER"] },
];
