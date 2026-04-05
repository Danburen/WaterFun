import type { PermissionType } from "~/api/permission";

export interface PermFormModel {
  name: string;
  code: string;
  description: string;
  type: PermissionType;
  resource: string;
  parentId: number | null;
  orderWeight: number | null;
  isSystem: boolean;
}

export interface PermFormExpose {
  validate: () => Promise<boolean>;
  resetFields: () => void;
}

