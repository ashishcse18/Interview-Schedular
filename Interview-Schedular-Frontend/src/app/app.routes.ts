import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./components/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./components/dashboard/dashboard.component').then((m) => m.DashboardComponent),
    canActivate: [authGuard],
  },
  {
    path: 'templates',
    loadComponent: () =>
      import('./components/templates/templates.component').then((m) => m.TemplatesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'send',
    loadComponent: () =>
      import('./components/campaign/campaign.component').then((m) => m.CampaignComponent),
    canActivate: [authGuard],
  },
  {
    path: 'logs',
    loadComponent: () => import('./components/logs/logs.component').then((m) => m.LogsComponent),
    canActivate: [authGuard],
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
