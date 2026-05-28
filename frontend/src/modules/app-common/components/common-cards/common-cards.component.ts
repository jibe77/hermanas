import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-common-cards',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './common-cards.component.html',
    styleUrls: ['common-cards.component.scss'],
})
export class CommonCardsComponent implements OnInit, OnDestroy {
    retryMessageIsDisplayed = false;

    @Input() notificationEvents: Observable<void>;
    @Output() serviceRetry = new EventEmitter();
    eventSubject: Subject<void> = new Subject<void>();
    private destroy$ = new Subject<void>();

    constructor(public _changeDetectorRef: ChangeDetectorRef) {}

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
