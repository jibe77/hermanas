import { User } from '@modules/auth/models';
export { User };

export class MockUser implements User {
    id = 'TEST_ID';
    login = 'TEST_LOGIN';
    email = 'TEST_EMAIL';
    backEndUser = 'TEST_BACKEND_USER';
    backEndPassword = 'TEST_BACKEND_PASSWORD';
    authState = 'TEST_AUTH_STATE';
}
