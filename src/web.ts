import { WebPlugin } from '@capacitor/core';

import type { CapacitorTipsPlugin, Product } from './definitions';

export class CapacitorTipsWeb extends WebPlugin implements CapacitorTipsPlugin {
  async listProducts(): Promise<{ products: Product[] }> {
    // Implement the listProducts logic here or use `unimplemented` if not implemented
    throw this.unimplemented('In-app purchases not available on web');
  }

  async purchaseProduct(): Promise<void> {
    // Implement the purchaseProduct logic here or use `unimplemented` if not implemented
    throw this.unimplemented('In-app purchases not available on web');
  }
}
