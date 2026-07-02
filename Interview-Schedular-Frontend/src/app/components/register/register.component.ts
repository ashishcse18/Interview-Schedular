import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  authService = inject(AuthService);
  toastService = inject(ToastService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  registerForm = {
    name: '',
    email: '',
    password: ''
  };

  isRegistering = false;

  onRegister() {
    if (!this.registerForm.name || !this.registerForm.email || !this.registerForm.password) return;

    this.isRegistering = true;
    this.cdr.detectChanges();

    this.authService.register(this.registerForm).subscribe({
      next: (res) => {
        this.toastService.success(res.message || 'Registration successful!');
        this.isRegistering = false;
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error(err);
        this.isRegistering = false;
        const errMsg = err.error?.message || 'Registration failed. Email might already be registered.';
        this.toastService.error(errMsg);
        this.cdr.detectChanges();
      }
    });
  }
}
