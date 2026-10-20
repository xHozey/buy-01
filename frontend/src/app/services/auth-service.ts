import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface loginReq {
  email: string;
  password: string;
}

export interface registerReq {
  email: string;
  name: string;
  password: string;
  role: 'CLIENT' | 'SELLER';
}

export interface loginRes {
  token: string;
}

export interface registerRes {
  id: string;
  name: string;
  email: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/auth';

  login(request: loginReq): Observable<loginRes> {
    return this.http.post<loginRes>(`${this.apiUrl}/login`, request);
  }

  register(request: registerReq): Observable<registerRes> {
    return this.http.post<registerRes>(`${this.apiUrl}/register`, request);
  }

  saveToken(token: string): void {
    localStorage.setItem('auth_token', 'Bearer ' + token);
  }
}
