import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    inject,
} from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CardViewDetailsComponent } from '../card-view-details/card-view-details.component';

@Component({
    selector: 'sb-common-cards',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './common-cards.component.html',
    styleUrls: ['common-cards.component.scss'],
    imports: [CardViewDetailsComponent],
})
export class CommonCardsComponent implements OnInit, OnDestroy {
    _changeDetectorRef = inject(ChangeDetectorRef);

    retryMessageIsDisplayed = false;

    @Input() notificationEvents: Observable<void>;
    @Output() serviceRetry = new EventEmitter();
    eventSubject: Subject<void> = new Subject<void>();
    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        this.notificationEvents.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.retryMessageIsDisplayed = true;
            this._changeDetectorRef.detectChanges();
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    onEvent(_event: any) {
        this.eventSubject.next();
        this.retry();
    }

    retry() {
        this.retryMessageIsDisplayed = false;
        this._changeDetectorRef.detectChanges();
        this.serviceRetry.emit();
    }
}
