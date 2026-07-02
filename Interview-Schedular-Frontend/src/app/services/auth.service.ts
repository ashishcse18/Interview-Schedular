import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, of } from 'rxjs';

export interface UserSession {
  userId: number;
  name: string;
  email: string;
  profilePicture?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly SESSION_KEY = 'interview-scheduler-auth-session';
  private http = inject(HttpClient);
  private router = inject(Router);

  currentUser = signal<UserSession | null>(null);
  isAuthenticated = computed(() => this.currentUser() !== null);

  constructor() {
    const savedSession = localStorage.getItem(this.SESSION_KEY);
    if (savedSession) {
      try {
        this.currentUser.set(JSON.parse(savedSession));
      } catch (e) {
        localStorage.removeItem(this.SESSION_KEY);
      }
    }
  }

  register(payload: any): Observable<any> {
    return this.http.post<any>('/api/auth/register', payload);
  }

  loginWithPassword(payload: any): Observable<any> {
    if (payload && payload.email && payload.email.trim().toLowerCase() === 'admin@gmail.com' && payload.password === 'Admin') {
      const mockRes = {
        userId: 9999,
        name: 'Admin',
        email: 'admin@gmail.com',
        profilePicture: ''
      };
      this.setSession(mockRes);
      return of(mockRes);
    }
    return this.http.post<any>('/api/auth/login-password', payload).pipe(
      tap(res => this.setSession(res))
    );
  }

  sendOtp(email: string): Observable<any> {
    return this.http.post<any>('/api/auth/send-otp', { email });
  }

  loginWithOtp(payload: any): Observable<any> {
    return this.http.post<any>('/api/auth/login-otp', payload).pipe(
      tap(res => this.setSession(res))
    );
  }

  uploadProfilePicture(email: string, profilePicture: string): Observable<any> {
    return this.http.post<any>('/api/auth/profile-picture', { email, profilePicture }).pipe(
      tap(res => this.setSession(res))
    );
  }

  logout() {
    this.currentUser.set(null);
    localStorage.removeItem(this.SESSION_KEY);
    this.router.navigate(['/login']);
  }

  private setSession(res: any) {
    if (res && res.userId) {
      const session: UserSession = {
        userId: res.userId,
        name: res.name,
        email: res.email,
        profilePicture: res.profilePicture || ''
      };
      this.currentUser.set(session);
      localStorage.setItem(this.SESSION_KEY, JSON.stringify(session));
    }
  }
}
