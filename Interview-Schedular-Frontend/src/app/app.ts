import { Component, ViewEncapsulation, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { ToastService } from './services/toast.service';
import { ThemeService, ThemeMode } from './services/theme.service';
import { AuthService } from './services/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
  encapsulation: ViewEncapsulation.None
})
export class App {
  pageTitle = 'Dashboard Overview';
  isThemeMenuOpen = false;
  isProfileMenuOpen = false;

  constructor(
    public toastService: ToastService,
    public themeService: ThemeService,
    public authService: AuthService,
    private router: Router
  ) {
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      const url = event.urlAfterRedirects || event.url;
      if (url.includes('/dashboard')) {
        this.pageTitle = 'Dashboard Overview';
      } else if (url.includes('/templates')) {
        this.pageTitle = 'WhatsApp Templates';
      } else if (url.includes('/send')) {
        this.pageTitle = 'Launch Campaign';
      } else if (url.includes('/logs')) {
        this.pageTitle = 'Campaign Logs';
      }
    });
  }

  get currentTheme(): ThemeMode {
    return this.themeService.theme();
  }

  toggleThemeMenu(event: Event) {
    event.stopPropagation();
    this.isThemeMenuOpen = !this.isThemeMenuOpen;
  }

  selectTheme(theme: ThemeMode) {
    this.themeService.setTheme(theme);
    this.isThemeMenuOpen = false;
  }

  toggleProfileMenu(event: Event) {
    event.stopPropagation();
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  onProfilePicSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    // Check size limit: 2MB
    if (file.size > 2 * 1024 * 1024) {
      this.toastService.error('Profile picture must be under 2MB.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const base64String = reader.result as string;
      const currentUser = this.authService.currentUser();
      if (currentUser && currentUser.email) {
        this.authService.uploadProfilePicture(currentUser.email, base64String).subscribe({
          next: () => {
            this.toastService.success('Profile picture updated!');
          },
          error: (err) => {
            console.error(err);
            this.toastService.error('Failed to upload profile picture.');
          }
        });
      }
    };
    reader.readAsDataURL(file);
  }

  onLogout() {
    this.isProfileMenuOpen = false;
    this.authService.logout();
  }

  @HostListener('document:click')
  closeMenus() {
    this.isThemeMenuOpen = false;
    this.isProfileMenuOpen = false;
  }
}
