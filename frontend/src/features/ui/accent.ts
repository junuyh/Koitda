// 카드마다 다른 색을 입히기 위한 액센트 팔레트(이미지 8 컬러풀 감성).
// Tailwind 는 소스에서 클래스 문자열을 스캔하므로 반드시 '완성된 문자열'을 나열한다(동적 조합 금지).

export type Accent = {
  /** 진한 배지(솔리드) */
  solid: string;
  /** 옅은 pill 배경 */
  soft: string;
  /** 이미지 자리 그라데이션 배경 */
  wash: string;
};

const PALETTE: Accent[] = [
  { solid: "bg-amber-400 text-amber-950", soft: "bg-amber-100 text-amber-800 dark:bg-amber-950/40 dark:text-amber-300", wash: "from-amber-100 to-amber-200 dark:from-amber-950/40 dark:to-amber-900/30" },
  { solid: "bg-rose-400 text-rose-950", soft: "bg-rose-100 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300", wash: "from-rose-100 to-rose-200 dark:from-rose-950/40 dark:to-rose-900/30" },
  { solid: "bg-sky-400 text-sky-950", soft: "bg-sky-100 text-sky-700 dark:bg-sky-950/40 dark:text-sky-300", wash: "from-sky-100 to-sky-200 dark:from-sky-950/40 dark:to-sky-900/30" },
  { solid: "bg-emerald-400 text-emerald-950", soft: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300", wash: "from-emerald-100 to-emerald-200 dark:from-emerald-950/40 dark:to-emerald-900/30" },
  { solid: "bg-violet-400 text-violet-950", soft: "bg-violet-100 text-violet-700 dark:bg-violet-950/40 dark:text-violet-300", wash: "from-violet-100 to-violet-200 dark:from-violet-950/40 dark:to-violet-900/30" },
  { solid: "bg-orange-400 text-orange-950", soft: "bg-orange-100 text-orange-700 dark:bg-orange-950/40 dark:text-orange-300", wash: "from-orange-100 to-orange-200 dark:from-orange-950/40 dark:to-orange-900/30" },
];

/** seed(보통 id)로 팔레트를 고정 선택 — 같은 항목은 항상 같은 색. */
export function accentOf(seed: number): Accent {
  const i = Math.abs(Math.trunc(seed)) % PALETTE.length;
  return PALETTE[i];
}
