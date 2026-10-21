import { Component, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Product, ProductService } from '../../services/product-service';
import { UserContextService } from '../../services/user-context-service';

@Component({
  selector: 'app-home',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly userContext = inject(UserContextService);
  readonly user = this.userContext.user;
  readonly userLoading = this.userContext.isLoading;
  readonly products = signal<Product[]>([]);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);

  ngOnInit(): void {
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  imageFor(product: Product): string {
    return product.imageUrls[0];
  }

  logout(): void {
    this.userContext.logout();
  }
}
