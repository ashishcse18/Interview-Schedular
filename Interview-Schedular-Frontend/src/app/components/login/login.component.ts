import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  authService = inject(AuthService);
  toastService = inject(ToastService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  activeTab: 'password' | 'otp' = 'password';

  // Form states
  passwordForm = {
    email: '',
    password: ''
  };

  otpForm = {
    email: '',
    otp: ''
  };

  // OTP flow state
  otpSent = false;
  isSendingOtp = false;
  isVerifyingOtp = false;
  testOtpCode = '';

  switchTab(tab: 'password' | 'otp') {
    this.activeTab = tab;
    this.cdr.detectChanges();
  }

  onPasswordLogin() {
    if (!this.passwordForm.email || !this.passwordForm.password) return;

    this.authService.loginWithPassword(this.passwordForm).subscribe({
      next: (res) => {
        this.toastService.success(`Welcome back, ${res.name}!`);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        const errMsg = err.error?.message || 'Login failed. Invalid email or password.';
        this.toastService.error(errMsg);
        this.cdr.detectChanges();
      }
    });
  }

  onRequestOtp() {
    if (!this.otpForm.email) return;

    this.isSendingOtp = true;
    this.testOtpCode = '';
    this.cdr.detectChanges();

    this.authService.sendOtp(this.otpForm.email).subscribe({
      next: (res) => {
        this.otpSent = true;
        this.isSendingOtp = false;
        if (res.otp) {
          this.testOtpCode = res.otp;
        }
        this.toastService.success('OTP sent successfully.');
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.isSendingOtp = false;
        const errMsg = err.error?.message || 'Failed to send OTP. Is this email registered?';
        this.toastService.error(errMsg);
        this.cdr.detectChanges();
      }
    });
  }

  onOtpLogin() {
    if (!this.otpForm.email || !this.otpForm.otp) return;

    this.isVerifyingOtp = true;
    this.cdr.detectChanges();

    this.authService.loginWithOtp(this.otpForm).subscribe({
      next: (res) => {
        this.toastService.success(`Welcome back, ${res.name}!`);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        this.isVerifyingOtp = false;
        const errMsg = err.error?.message || 'Invalid or expired OTP. Please try again.';
        this.toastService.error(errMsg);
        this.cdr.detectChanges();
      }
    });
  }
}
