"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { ApiError } from "@/lib/api";
import { authApi } from "@/features/auth/api";
import { loginSchema, type LoginInput } from "@/features/auth/schemas";
import { Field, inputClass, buttonClass } from "@/components/form";
import { KakaoLoginButton } from "@/features/auth/KakaoLoginButton";

export default function LoginPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({ resolver: zodResolver(loginSchema) });

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      router.push("/");
    },
    onError: (error) => {
      const message =
        error instanceof ApiError ? error.message : "로그인 중 오류가 발생했습니다.";
      setError("root", { message });
    },
  });

  return (
    <main className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <h1 className="text-2xl font-semibold tracking-tight">로그인</h1>
        <p className="mt-1 text-sm text-neutral-500">코잇다에 오신 것을 환영합니다.</p>

        <form
          className="mt-8 space-y-4"
          onSubmit={handleSubmit((values) => mutation.mutate(values))}
          noValidate
        >
          <Field id="email" label="이메일" error={errors.email?.message}>
            <input
              id="email"
              type="email"
              autoComplete="email"
              className={inputClass}
              {...register("email")}
            />
          </Field>

          <Field id="password" label="비밀번호" error={errors.password?.message}>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              className={inputClass}
              {...register("password")}
            />
          </Field>

          {errors.root?.message && (
            <p role="alert" className="text-sm text-red-600">
              {errors.root.message}
            </p>
          )}

          <button type="submit" disabled={isSubmitting} className={buttonClass}>
            {isSubmitting ? "로그인 중…" : "로그인"}
          </button>
        </form>

        <div className="my-5 flex items-center gap-3 text-xs text-neutral-400">
          <span className="h-px flex-1 bg-neutral-200 dark:bg-neutral-800" /> 또는 <span className="h-px flex-1 bg-neutral-200 dark:bg-neutral-800" />
        </div>
        <KakaoLoginButton label="카카오로 로그인" />

        <p className="mt-6 text-center text-sm text-neutral-500">
          아직 계정이 없으신가요?{" "}
          <Link href="/signup" className="font-medium text-neutral-900 underline dark:text-neutral-100">
            회원가입
          </Link>
        </p>
      </div>
    </main>
  );
}
