import { get, post } from '../../../services/api.js';

export const authService = {
  me: () => get('/auth/me'),
  login: (username, password) => post('/auth/login', { username, password }),
  logout: () => post('/auth/logout'),
};
