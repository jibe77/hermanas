import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ErrorHandler, NgModule, isDevMode } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {
    authInterceptor,
    loadingInterceptor,
    loggingInterceptor,
    retryInterceptor,
} from '@common/interceptors';
import { GlobalErrorHandler } from '@common/services';
import { ProgressWebsocketService } from '@modules/dashboard/services/progresswebsocket.service';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ServiceWorkerModule } from '@angular/service-worker';

@NgModule({
    declarations: [AppComponent],
    bootstrap: [AppComponent],
    imports: [
        BrowserModule,
        AppRoutingModule,
        BrowserAnimationsModule,
        NgbModule,
        ServiceWorkerModule.register('ngsw-worker.js', {
            enabled: !isDevMode(),
            // Register the ServiceWorker as soon as the application is stable
            // or after 30 seconds (whichever comes first).
            registrationStrategy: 'registerWhenStable:30000',
        }),
    ],
    providers: [
        ProgressWebsocketService,
        { provide: ErrorHandler, useClass: GlobalErrorHandler },
        provideHttpClient(
            withInterceptors([
                loadingInterceptor,
                loggingInterceptor,
                authInterceptor,
                retryInterceptor,
            ])
        ),
    ],
})
export class AppModule {}
