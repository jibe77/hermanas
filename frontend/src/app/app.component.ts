import { OnInit, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ChildActivationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit, OnDestroy {
    title = 'hermanas-client';
    private destroy$ = new Subject<void>();

    constructor(
        public router: Router,
        private titleService: Title,
        private ref: ChangeDetectorRef
    ) {}

    ngOnInit() {
        this.router.events
            .pipe(
                filter(event => event instanceof ChildActivationEnd),
                takeUntil(this.destroy$)
            )
            .subscribe(event => {
                let snapshot = (event as ChildActivationEnd).snapshot;
                while (snapshot.firstChild !== null) {
                    snapshot = snapshot.firstChild;
                }
                this.titleService.setTitle(snapshot.data.title || 'Hermanas');
            });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
