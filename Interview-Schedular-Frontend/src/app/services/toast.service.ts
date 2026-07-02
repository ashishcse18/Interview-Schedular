import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts = signal<Toast[]>([]);

  show(message: string, type: 'success' | 'error' | 'info' = 'info') {
    const id = 'toast-' + Math.random().toString(36).substring(2, 11);
    const newToast: Toast = { id, message, type };
    this.toasts.update(t => [...t, newToast]);

    setTimeout(() => {
      this.remove(id);
    }, 4500);
  }

  success(message: string) {
    this.show(message, 'success');
  }

  error(message: string) {
    this.show(message, 'error');
  }

  info(message: string) {
    this.show(message, 'info');
  }

  remove(id: string) {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}
