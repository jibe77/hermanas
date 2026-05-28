import { OnInit, ChangeDetectorRef, Component, OnDestroy, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ChildActivationEnd, Router, RouterOutlet } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    imports: [RouterOutlet],
})
export class AppComponent implements OnInit, OnDestroy {
    router = inject(Router);
    private titleService = inject(Title);
    private ref = inject(ChangeDetectorRef);

    title = 'hermanas-client';
    private destroy$ = new Subject<void>();

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
