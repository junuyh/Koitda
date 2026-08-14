"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { ApiError } from "@/lib/api";
import { authApi } from "@/features/auth/api";
import { signupSchema, type SignupInput } from "@/features/auth/schemas";
import { Field, inputClass, buttonClass } from "@/components/form";
import { KakaoLoginButton } from "@/features/auth/KakaoLoginButton";
import {
  MARKETING_INFO,
  PRIVACY_POLICY,
  SERVICE_TERMS,
  TermsDocument,
} from "@/features/auth/consentDocuments";

export default function SignupPage() {
  const router = useRouter();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignupInput>({
    resolver: zodResolver(signupSchema),
    defaultValues: { agreeService: false, agreePrivacy: false, agreeMarketing: false },
  });

  const mutation = useMutation({
    mutationFn: authApi.signup,
    onSuccess: () => {
      // 가입은 세션을 만들지 않는다. 로그인 화면으로 이동해 로그인하게 한다.
      router.push("/login?registered=1");
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === "EMAIL_DUPLICATED") {
          setError("email", { message: error.message });
          return;
        }
        if (error.code === "NICKNAME_DUPLICATED") {
          setError("nickname", { message: error.message });
          return;
        }
        setError("root", { message: error.message });
        return;
      }
      setError("root", { message: "회원가입 중 오류가 발생했습니다." });
    },
  });

  return (
    <main className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <h1 className="text-2xl font-semibold tracking-tight">회원가입</h1>
        <p className="mt-1 text-sm text-neutral-500">코잇다 계정을 만들어 보세요.</p>

        <form
          className="mt-8 space-y-4"
          onSubmit={handleSubmit((values) => mutation.mutate(values))}
          noValidate
        >
          <Field id="email" label="이메일" error={errors.email?.message}>
            <input id="email" type="email" autoComplete="email" className={inputClass} {...register("email")} />
          </Field>

          <Field id="nickname" label="닉네임" error={errors.nickname?.message}>
            <input id="nickname" type="text" autoComplete="nickname" className={inputClass} {...register("nickname")} />
          </Field>

          <Field id="password" label="비밀번호 (8자 이상)" error={errors.password?.message}>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              className={inputClass}
              {...register("password")}
            />
          </Field>

          <fieldset className="space-y-2 pt-2">
            <legend className="sr-only">약관 동의</legend>
            <CheckboxRow
              id="agreeService"
              error={errors.agreeService?.message}
              label="(필수) 서비스 이용약관에 동의합니다."
              register={register("agreeService")}
            />
            <TermsDocument title="서비스 이용약관" body={SERVICE_TERMS} />
            <CheckboxRow
              id="agreePrivacy"
              error={errors.agreePrivacy?.message}
              label="(필수) 개인정보 수집·이용에 동의합니다."
              register={register("agreePrivacy")}
            />
            <TermsDocument title="개인정보 처리방침" body={PRIVACY_POLICY} />
            <CheckboxRow
              id="agreeMarketing"
              label="(선택) 마케팅 정보 수신에 동의합니다."
              register={register("agreeMarketing")}
            />
            <TermsDocument title="마케팅 정보 수신" body={MARKETING_INFO} />
          </fieldset>

          {errors.root?.message && (
            <p role="alert" className="text-sm text-red-600">
              {errors.root.message}
            </p>
          )}

          <button type="submit" disabled={isSubmitting} className={buttonClass}>
            {isSubmitting ? "가입 중…" : "가입하기"}
          </button>
        </form>

        <div className="my-5 flex items-center gap-3 text-xs text-neutral-400">
          <span className="h-px flex-1 bg-neutral-200 dark:bg-neutral-800" /> 또는 <span className="h-px flex-1 bg-neutral-200 dark:bg-neutral-800" />
        </div>
        <KakaoLoginButton label="카카오로 시작하기" />

        <p className="mt-6 text-center text-sm text-neutral-500">
          이미 계정이 있으신가요?{" "}
          <Link href="/login" className="font-medium text-neutral-900 underline dark:text-neutral-100">
            로그인
          </Link>
        </p>
      </div>
    </main>
  );
}

function CheckboxRow({
  id,
  label,
  error,
  register,
}: {
  id: string;
  label: string;
  error?: string;
  register: UseFormRegisterReturn;
}) {
  return (
    <div>
      <label htmlFor={id} className="flex items-center gap-2 text-sm">
        <input id={id} type="checkbox" className="h-4 w-4" {...register} />
        <span>{label}</span>
      </label>
      {error && (
        <p role="alert" className="mt-1 text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
