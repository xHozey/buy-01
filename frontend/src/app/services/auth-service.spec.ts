import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthService, loginReq, registerReq } from './auth-service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => httpTesting.verify());

  it('posts login credentials and returns the token response', () => {
    const request: loginReq = { email: 'client@example.com', password: 'secret' };
    const response = { token: 'jwt-token' };
    let actual: typeof response | undefined;

    service.login(request).subscribe((value) => (actual = value));

    const req = httpTesting.expectOne('https://localhost:8080/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);
    expect(actual).toEqual(response);
  });

  it('posts registration data and returns the created user', () => {
    const request: registerReq = {
      email: 'seller@example.com',
      name: 'Seller',
      password: 'secret',
      role: 'SELLER',
    };
    const response = { id: 'user-1', name: 'Seller', email: request.email };
    let actual: typeof response | undefined;

    service.register(request).subscribe((value) => (actual = value));

    const req = httpTesting.expectOne('https://localhost:8080/api/v1/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);
    expect(actual).toEqual(response);
  });

  it('saves the token with the Bearer prefix', () => {
    service.saveToken('jwt-token');

    expect(localStorage.getItem('auth_token')).toBe('Bearer jwt-token');
  });
});
