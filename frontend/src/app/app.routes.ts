import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Home } from './pages/home/home';
import { Dashboard } from './pages/dashboard/dashboard';
import { ProductDetail } from './pages/product-detail/product-detail';

export const routes: Routes = [
  {
    path: 'login',
    component: Login,
  },
  {
    path: 'register',
    component: Register,
  },
  {
    path: 'products/:id',
    component: ProductDetail,
  },
  {
    path: 'dashboard',
    component: Dashboard,
  },
  {
    path: '',
    component: Home,
  },
  {
    path: '**',
    redirectTo: '',
  },
];
