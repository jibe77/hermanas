import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MusicService } from '@modules/dashboard/services/music.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('MusicService', () => {
    let service: MusicService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                MusicService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        service = TestBed.inject(MusicService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
