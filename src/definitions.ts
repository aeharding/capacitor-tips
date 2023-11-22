export interface CapacitorTipsPlugin {
  listProducts(options: {
    productIdentifiers: string[];
  }): Promise<{ products: Product[] }>;
  purchaseProduct(product: Product): Promise<void>;
}

export interface Product {
  identifier: string;

  /**
   * Locale formatted price
   *
   * ex `$3.99`
   */
  priceString: string;

  /**
   * ex. `3.99`
   */
  price: number;

  description: string;
  name: string;
}
