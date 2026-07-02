import { Injectable, signal, effect } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'app-theme-preference';
  
  theme = signal<ThemeMode>('system');
  
  private mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

  constructor() {
    // Load persisted theme or default to system
    const savedTheme = localStorage.getItem(this.THEME_KEY) as ThemeMode;
    if (savedTheme) {
      this.theme.set(savedTheme);
    }
    
    // Apply changes whenever theme changes
    effect(() => {
      const mode = this.theme();
      localStorage.setItem(this.THEME_KEY, mode);
      this.applyTheme(mode);
    });

    // Listen for OS system theme changes
    this.mediaQuery.addEventListener('change', () => {
      if (this.theme() === 'system') {
        this.applyTheme('system');
      }
    });
  }

  setTheme(mode: ThemeMode) {
    this.theme.set(mode);
  }

  private applyTheme(mode: ThemeMode) {
    let resolvedTheme: 'light' | 'dark' = 'light';
    
    if (mode === 'system') {
      resolvedTheme = this.mediaQuery.matches ? 'dark' : 'light';
    } else {
      resolvedTheme = mode;
    }
    
    document.documentElement.setAttribute('data-theme', resolvedTheme);
  }
}
