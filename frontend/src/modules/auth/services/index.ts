import { AuthService } from './auth.service';
import { LoginService } from './login.service';
import { UserService } from './user.service';

export const services = [AuthService, LoginService, UserService];

export * from './auth.service';
export * from './login.service';
export * from './user.service';
