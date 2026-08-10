import { z } from "zod";

// 서버 검증(SignupRequest)과 규칙을 맞춘다. 최종 검증은 서버가 한다.
export const signupSchema = z.object({
  email: z.email("올바른 이메일을 입력하세요."),
  password: z
    .string()
    .min(8, "비밀번호는 8자 이상이어야 합니다.")
    .max(72, "비밀번호는 72자 이하여야 합니다."),
  nickname: z
    .string()
    .min(1, "닉네임을 입력하세요.")
    .max(30, "닉네임은 30자 이하여야 합니다."),
  // 체크박스는 boolean 으로 두고 refine 으로 '동의(true)'를 강제한다.
  // z.literal(true) 로 하면 추론 타입이 true 가 되어 RHF defaultValues(false)와 충돌한다.
  agreeService: z.boolean().refine((v) => v === true, { message: "서비스 이용약관에 동의해야 합니다." }),
  agreePrivacy: z.boolean().refine((v) => v === true, { message: "개인정보 처리방침에 동의해야 합니다." }),
  agreeMarketing: z.boolean(),
});

export type SignupInput = z.infer<typeof signupSchema>;

export const loginSchema = z.object({
  email: z.email("올바른 이메일을 입력하세요."),
  password: z.string().min(1, "비밀번호를 입력하세요."),
});

export type LoginInput = z.infer<typeof loginSchema>;
