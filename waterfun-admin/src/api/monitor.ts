import request from "~/utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response"

export interface CpuInfoVO {
  name?: string
  physicalCores?: number
  logicalCores?: number
  usage?: number
  load1m?: number
  load5m?: number
  load15m?: number
}

export interface MemoryInfoVO {
  total?: number
  used?: number
  available?: number
  usage?: number
}

export interface DiskInfoVO {
  mount?: string
  type?: string
  total?: number
  used?: number
  free?: number
  usage?: number
}

export interface Jvm {
  name?: string
  version?: string
  uptime?: number
}

export interface Heap {
  used?: number
  committed?: number
  max?: number
}

export interface NonHeap {
  used?: number
}

export interface Thread {
  count?: number
}

export interface Os {
  availableProcessors?: number
  totalMemory?: number
  freeMemory?: number
}

export interface JvmInfoVO {
  source?: string
  jvm?: Jvm
  heap?: Heap
  nonHeap?: NonHeap
  thread?: Thread
  os?: Os
}

export interface MergedJvmInfo {
  admin?: JvmInfoVO
  user?: JvmInfoVO
  userError?: string
}

export interface NetworkInfoVO {
  sent?: number
  received?: number
  timestamp?: number
}

export interface SystemDetailVO {
  os?: string
  hostname?: string
  arch?: string
  uptime?: number
}

export interface SystemInfoVO {
  cpu?: CpuInfoVO
  memory?: MemoryInfoVO
  disks?: DiskInfoVO[]
  network?: NetworkInfoVO
  system?: SystemDetailVO
  jvms?: MergedJvmInfo
}

export const getSystemMonitor = (): PromiseResBody<SystemInfoVO> => {
  return request.get<SystemInfoVO>('/monitor/system')
}

export const getJvms = (): PromiseResBody<MergedJvmInfo> => {
  return request.get<MergedJvmInfo>('/monitor/jvms')
}
