import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { AuthService, registerReq } from '../../services/auth-service';

@Component({
  selector: 'app-register',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly registerForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(900)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(999)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(260)]],
    role: ['CLIENT' as registerReq['role'], Validators.required],
  });
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.authService.register(this.registerForm.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: (error: { error?: { message?: string } }) => {
        this.errorMessage.set(error.error?.message ?? 'Unable to create your account. Please try again.');
        this.isSubmitting.set(false);
      },
    });
  }
}
