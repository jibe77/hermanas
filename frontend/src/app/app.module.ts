import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { APP_INITIALIZER, ErrorHandler, NgModule, isDevMode } from '@angular/core';
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
import { UserService } from '@modules/auth/services/user.service';
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
        // Block Angular bootstrap until /auth/me has answered, so AuthGuard never has to
        // decide on an "unknown" session state. Without this, navigating directly to a
        // protected route on a cold load would flash the login page before checkAuthState
        // resolves and the user is recognised as signed-in.
        {
            provide: APP_INITIALIZER,
            multi: true,
            deps: [UserService],
            useFactory: (userService: UserService) => () => userService.initialAuthCheck(),
        },
    ],
})
export class AppModule {}
