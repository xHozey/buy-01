import { DecimalPipe } from '@angular/common';
import { Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MediaService } from '../../services/media-service';
import { Product, ProductRequest, ProductService } from '../../services/product-service';
import { UserContextService } from '../../services/user-context-service';

interface ImagePreview {
  file: File;
  url: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnDestroy, OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly mediaService = inject(MediaService);
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);
  private readonly userContext = inject(UserContextService);

  @ViewChild('productImagesInput') private productImagesInput?: ElementRef<HTMLInputElement>;

  readonly user = this.userContext.user;
  readonly products = signal<Product[]>([]);
  readonly selectedFiles = signal<File[]>([]);
  readonly imagePreviews = signal<ImagePreview[]>([]);
  readonly editingProduct = signal<Product | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly productForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    price: [0, [Validators.required, Validators.min(0.1)]],
    quantity: [0, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.loadProducts();
  }

  ngOnDestroy(): void {
    this.revokePreviewUrls();
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.productService.getMyProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          'Your products could not be loaded. Make sure you are signed in as a seller.',
        );
        this.isLoading.set(false);
      },
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.setSelectedFiles(input.files ? Array.from(input.files) : []);
  }

  removeSelectedFile(index: number): void {
    const files = this.selectedFiles().filter((_, fileIndex) => fileIndex !== index);
    this.setSelectedFiles(files);
    if (this.productImagesInput) {
      this.productImagesInput.nativeElement.value = '';
    }
  }

  editProduct(product: Product): void {
    this.editingProduct.set(product);
    this.productForm.setValue({
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
    });
    this.clearSelectedFiles();
    this.errorMessage.set('');
    this.successMessage.set('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.editingProduct.set(null);
    this.productForm.reset({ name: '', description: '', price: 0, quantity: 0 });
    this.clearSelectedFiles();
    this.errorMessage.set('');
  }

  submitProduct(): void {
    if (this.productForm.invalid || this.isSubmitting()) {
      this.productForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    const product = this.editingProduct();
    const files = this.selectedFiles();
    const imageUrls = product?.imageUrls ?? [];
    const upload$ = files.length ? this.mediaService.uploadImages(files) : null;

    if (upload$) {
      upload$.subscribe({
        next: ({ urls }) => this.saveProduct({ imageUrls: urls }, product),
        error: () => this.finishWithError('Images could not be uploaded. Please try again.'),
      });
      return;
    }

    this.saveProduct({ imageUrls }, product);
  }

  deleteProduct(product: Product): void {
    if (!window.confirm(`Delete ${product.name}? This cannot be undone.`)) {
      return;
    }

    this.errorMessage.set('');
    this.productService.deleteProduct(product.id).subscribe({
      next: () => {
        this.products.update((products) => products.filter((item) => item.id !== product.id));
        if (this.editingProduct()?.id === product.id) {
          this.cancelEdit();
        }
      },
      error: () => this.errorMessage.set('This product could not be deleted. Please try again.'),
    });
  }

  logout(): void {
    this.userContext.logout();
    this.router.navigateByUrl('/');
  }

  private saveProduct(upload: { imageUrls: string[] }, product: Product | null): void {
    const values = this.productForm.getRawValue();
    const request: ProductRequest = {
      ...values,
      imageUrls: product && !this.selectedFiles().length ? product.imageUrls : upload.imageUrls,
    };
    const save$ = product
      ? this.productService.updateProduct(product.id, request)
      : this.productService.createProduct(request);

    save$.subscribe({
      next: (savedProduct) => {
        this.products.update((products) =>
          product
            ? products.map((item) => (item.id === savedProduct.id ? savedProduct : item))
            : [savedProduct, ...products],
        );
        this.successMessage.set(product ? 'Product updated.' : 'Product created.');
        this.cancelEdit();
        this.isSubmitting.set(false);
      },
      error: () =>
        this.finishWithError(
          'The product could not be saved. Please check the details and try again.',
        ),
    });
  }

  private finishWithError(message: string): void {
    this.errorMessage.set(message);
    this.isSubmitting.set(false);
  }

  private setSelectedFiles(files: File[]): void {
    this.revokePreviewUrls();
    this.selectedFiles.set(files);
    this.imagePreviews.set(files.map((file) => ({ file, url: URL.createObjectURL(file) })));
  }

  private clearSelectedFiles(): void {
    this.setSelectedFiles([]);
    if (this.productImagesInput) {
      this.productImagesInput.nativeElement.value = '';
    }
  }

  private revokePreviewUrls(): void {
    this.imagePreviews().forEach(({ url }) => URL.revokeObjectURL(url));
    this.imagePreviews.set([]);
  }
}
