export interface QueryError {
  message: string;
}

export async function queryResult<T>(
  request: Promise<T>,
): Promise<{ data: T } | { error: QueryError }> {
  try {
    return { data: await request };
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
