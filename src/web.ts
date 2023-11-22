import { WebPlugin } from '@capacitor/core';

import type { CapacitorTipsPlugin } from './definitions';

export class CapacitorTipsWeb extends WebPlugin implements CapacitorTipsPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
