export interface RoleFormModel {
  name: string;
  code: string;
  description: string;
  parentId: number | null;
  orderWeight: number | null;
  isSystem: boolean;
}

export interface RoleFormExpose {
  validate: () => Promise<boolean>;
  resetFields: () => void;
}

