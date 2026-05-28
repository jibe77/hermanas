import { Component, EventEmitter, Output } from '@angular/core';
import { NgbDropdown } from '@ng-bootstrap/ng-bootstrap';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

@Component({
    selector: 'sb-dashboard-webcam-action',
    templateUrl: './dashboard-webcam-action.component.html',
    styleUrls: ['dashboard-webcam-action.component.scss'],
    imports: [NgbDropdown, RouterLink, FaIconComponent],
})
export class DashboardWebcamActionComponent {
    @Output() refreshWebcamEvent = new EventEmitter();

    constructor() {}

    public refreshPicture() {
        this.refreshWebcamEvent.emit('picture');
    }
}
