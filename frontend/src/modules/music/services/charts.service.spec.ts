import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ChartsService } from './charts.service';

describe('ChartsService', () => {
    let chartsService: ChartsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [ChartsService],
        });
        chartsService = TestBed.inject(ChartsService);
    });

    it('should be created', () => {
        expect(chartsService).toBeTruthy();
    });
});
