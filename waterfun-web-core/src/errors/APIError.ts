export type APIErrorCode = string;

const toI18nKey = (rawCode: string): string => {
  if (!rawCode) return 'error.general.unknown_error';
  if (rawCode.startsWith('error.') || rawCode.startsWith('message.')) return rawCode;
  return `error.${rawCode}`;
};

const httpStatusToRawCode = (status: number): string => {
  switch (status) {
    case 200:
      return 'http.success';
    case 400:
      return 'http.bad_request';
    case 401:
      return 'http.unauthorized';
    case 403:
      return 'http.forbidden';
    case 404:
      return 'http.not_found';
    case 409:
      return 'http.conflict';
    case 500:
      return 'http.internal_server_error';
    default:
      return 'general.unknown_error';
  }
};

export interface APIErrorInit {
  code: APIErrorCode;
  rawCode?: string;
  httpStatus?: number;
  raw?: unknown;
  errors?: string[];
  timestamp?: unknown;
  message?: string;
}

export class APIError extends Error {
  code: APIErrorCode;
  rawCode?: string;
  httpStatus?: number;
  raw?: unknown;
  errors?: string[];
  timestamp?: unknown;

  constructor(init: APIErrorInit) {
    super(init.message ?? init.code);
    this.name = 'APIError';
    this.code = init.code;

    if (init.rawCode !== undefined) this.rawCode = init.rawCode;
    if (init.httpStatus !== undefined) this.httpStatus = init.httpStatus;
    if (init.raw !== undefined) this.raw = init.raw;
    if (init.errors !== undefined) this.errors = init.errors;
    if (init.timestamp !== undefined) this.timestamp = init.timestamp;
  }

  static fromHttp(init: {
    code?: unknown;
    httpStatus?: number;
    raw?: unknown;
  }): APIError {
    const rawObj = (init.raw && typeof init.raw === 'object') ? (init.raw as Record<string, unknown>) : undefined;

    const rawCode = String(
      init.code
        ?? rawObj?.code
        ?? (init.httpStatus != null ? httpStatusToRawCode(init.httpStatus) : 'general.unknown_error')
    );

    const code = toI18nKey(rawCode);
    const errors = Array.isArray(rawObj?.errors) ? rawObj?.errors.map(String) : undefined;
    const timestamp = rawObj?.timestamp;

    const apiInit: APIErrorInit = { code, rawCode };
    if (init.httpStatus !== undefined) apiInit.httpStatus = init.httpStatus;
    if (init.raw !== undefined) apiInit.raw = init.raw;
    if (errors !== undefined) apiInit.errors = errors;
    if (timestamp !== undefined) apiInit.timestamp = timestamp;

    return new APIError(apiInit);
  }
}
