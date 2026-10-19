import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export type UserRole = 'CLIENT' | 'SELLER';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  name: string;
  role: UserRole;
}

export interface LoginResponse {
  token: string;
}

export interface RegisterResponse {
  id: string;
  name: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authUrl = 'http://localhost:8080/api/v1/auth';
  private readonly tokenKey = 'auth-token';

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, request).pipe(
      tap(({ token }) => localStorage.setItem(this.tokenKey, token)),
    );
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.authUrl}/register`, request);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }
}