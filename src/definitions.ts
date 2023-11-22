export interface CapacitorTipsPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
