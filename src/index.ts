import { registerPlugin } from '@capacitor/core';

import type { CapacitorTipsPlugin } from './definitions';

const CapacitorTips = registerPlugin<CapacitorTipsPlugin>('CapacitorTips', {
  web: () => import('./web').then(m => new m.CapacitorTipsWeb()),
});

export * from './definitions';
export { CapacitorTips };
