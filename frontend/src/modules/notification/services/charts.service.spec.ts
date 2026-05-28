import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ChartsService, HermanasUser, UserCreate, UserUpdate } from './charts.service';

describe('Notification ChartsService (Users CRUD)', () => {
    let service: ChartsService;
    let httpMock: HttpTestingController;

    const adminUser: HermanasUser = {
        login: 'marguerite',
        email: 'marguerite@coop.local',
        role: 'ADMIN',
        notificationsEnabled: true,
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(ChartsService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    describe('me', () => {
        it('GETs /users/me', async () => {
            const result = new Promise<HermanasUser>(resolve => service.me().subscribe(resolve));

            const req = httpMock.expectOne(r => r.url.endsWith('/users/me'));
            expect(req.request.method).toBe('GET');
            req.flush(adminUser);

            await expect(result).resolves.toEqual(adminUser);
        });
    });

    describe('updateMe', () => {
        it('PUTs the patch to /users/me', async () => {
            const patch: UserUpdate = { email: 'new@coop.local', notificationsEnabled: false };
            const result = new Promise<HermanasUser>(resolve =>
                service.updateMe(patch).subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/users/me'));
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual(patch);
            req.flush({ ...adminUser, ...patch });

            await result;
        });
    });

    describe('list', () => {
        it('GETs /users and returns the array', async () => {
            const result = new Promise<HermanasUser[]>(resolve =>
                service.list().subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/users'));
            expect(req.request.method).toBe('GET');
            req.flush([adminUser]);

            await expect(result).resolves.toEqual([adminUser]);
        });
    });

    describe('create', () => {
        it('POSTs the new user to /users', async () => {
            const payload: UserCreate = {
                login: 'alice',
                password: 'secret',
                email: 'alice@coop.local',
                role: 'USER',
            };
            const result = new Promise<HermanasUser>(resolve =>
                service.create(payload).subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/users'));
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(payload);
            req.flush({
                login: 'alice',
                email: 'alice@coop.local',
                role: 'USER',
                notificationsEnabled: false,
            });

            await result;
        });
    });

    describe('update', () => {
        it('PUTs to /users/<login> with the URL-encoded login', async () => {
            const result = new Promise<HermanasUser>(resolve =>
                service.update('alice', { role: 'ADMIN' }).subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/users/alice'));
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual({ role: 'ADMIN' });
            req.flush({ ...adminUser, login: 'alice', role: 'ADMIN' });

            await result;
        });

        it('encodes special characters in the login', async () => {
            new Promise<HermanasUser>(resolve =>
                service.update('a/b c', { role: 'USER' }).subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/users/a%2Fb%20c'));
            expect(req.request.method).toBe('PUT');
            req.flush(adminUser);
        });
    });

    describe('delete', () => {
        it('DELETEs /users/<login>', async () => {
            const result = new Promise<void>(resolve =>
                service.delete('alice').subscribe(() => resolve())
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/users/alice'));
            expect(req.request.method).toBe('DELETE');
            req.flush(null);

            await result;
        });
    });
});
