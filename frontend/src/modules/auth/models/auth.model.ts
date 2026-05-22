export interface User {
    id: string;
    login: string;
    email: string;
    backEndUser?: string;
    backEndPassword?: string;
    authState: string;
    roles?: string[];
}
