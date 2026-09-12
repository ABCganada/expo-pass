export interface QueryError {
  message: string;
}

export async function queryResult<T>(
  request: Promise<T>,
): Promise<{ data: T } | { error: QueryError }> {
  try {
    const result = await request;
    // RTK Query v2: data must not be undefined (void mutations return undefined)
    return { data: (result === undefined ? null : result) as T };
  } catch (reason: unknown) {
    return {
      error: {
        message: reason instanceof Error
          ? reason.message
          : "요청을 처리하지 못했습니다.",
      },
    };
  }
}

export function queryErrorMessage(
  error: unknown,
  fallback: string,
): string {
  if (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    typeof error.message === "string"
  ) {
    return error.message;
  }
  return fallback;
}
