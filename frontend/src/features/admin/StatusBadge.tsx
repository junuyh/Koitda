const PATTERN_LABEL: Record<string, string> = {
  DRAFT: "임시저장",
  PENDING: "심사 중",
  APPROVED: "판매 중",
  REJECTED: "반려",
  SUSPENDED: "정지",
};

const APPLICATION_LABEL: Record<string, string> = {
  PENDING: "심사 중",
  APPROVED: "승인",
  REJECTED: "반려",
};

const COLOR: Record<string, string> = {
  DRAFT: "bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300",
  PENDING: "bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300",
  APPROVED: "bg-green-100 text-green-700 dark:bg-green-950/40 dark:text-green-300",
  REJECTED: "bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300",
  SUSPENDED: "bg-neutral-200 text-neutral-600",
};

function Badge({ status, label }: { status: string; label: string }) {
  return (
    <span className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-medium ${COLOR[status] ?? ""}`}>{label}</span>
  );
}

export function PatternStatusBadge({ status }: { status: string }) {
  return <Badge status={status} label={PATTERN_LABEL[status] ?? status} />;
}

export function ApplicationStatusBadge({ status }: { status: string }) {
  return <Badge status={status} label={APPLICATION_LABEL[status] ?? status} />;
}
