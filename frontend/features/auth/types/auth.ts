export interface CurrentUser {
  /** 서버가 문자열로 내려준다. users.id 가 2^53 을 넘어 숫자로는 값이 깨진다. */
  id: string;
  authUuid: string;
  name: string;
  email: string;
  roles: string[];
}

export type MeResponse = CurrentUser;
