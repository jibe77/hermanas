import { HttpErrorInterceptor } from '@modules/dashboard/interceptors/http-error.interceptor';

export const interceptors = [HttpErrorInterceptor];

export * from './http-error.interceptor';
