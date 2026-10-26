import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  avatar: string | null;
  role: 'CLIENT' | 'SELLER';
}

@Injectable({ providedIn: 'root' })
export class UserContextService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/users`;
  private readonly userSignal = signal<UserProfile | null>(null);

  readonly user = this.userSignal.asReadonly();
  readonly isLoading = signal(true);

  load(): Observable<void> {
    const token = localStorage.getItem('auth_token');
    if (!token) {
      this.isLoading.set(false);
      return of(void 0);
    }

    this.isLoading.set(true);
    return this.http.get<UserProfile>(`${this.apiUrl}/me`).pipe(
      tap((user) => this.userSignal.set(user)),
      catchError(() => {
        localStorage.removeItem('auth_token');
        this.userSignal.set(null);
        return of(null);
      }),
      finalize(() => this.isLoading.set(false)),
      map(() => void 0),
    );
  }

  updateProfile(
    profile: Omit<UserProfile, 'id' | 'role'> & { password: string },
  ): Observable<UserProfile> {
    return this.http
      .put<UserProfile>(`${this.apiUrl}/me`, profile)
      .pipe(tap((user) => this.userSignal.set(user)));
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    this.userSignal.set(null);
  }
}
