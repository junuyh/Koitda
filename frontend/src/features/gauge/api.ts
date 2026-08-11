import { apiFetch } from "@/lib/api";

export type GaugeInput = { stitches: number | null; rows: number | null; needleSizeMm: number | null };
export type SizeInfo = { label: string; castOnStitches: number | null; measurements: Record<string, number> };
export type MyGaugeSuggestion = { projectGaugeId: number; stitches: number | null; rows: number | null; measuredStage: string | null };

export type GaugeDefaults = {
  patternSource: string;
  requiresManualInput: boolean;
  patternGauge: GaugeInput | null;
  sizes: SizeInfo[];
  measurementLabels: Record<string, string>;
  myGaugeSuggestions: MyGaugeSuggestion[];
};

export type GaugeAdjustment = {
  adjustedCastOnStitches: number;
  estimatedMeasurements: Record<string, number>;
  differenceFromPattern: Record<string, number>;
  formula: string;
};

export type SizeAdjustment = {
  key: string;
  label: string;
  patternValue: number;
  targetValue: number;
  differenceCm: number;
  requiredStitches: number;
  formula: string;
};

export type NeedleRecommendation = { direction: string; suggestedMm: number | null; note: string };

export type CalculationResult = {
  calculationId: number;
  gaugeAdjustment: GaugeAdjustment;
  sizeAdjustments: SizeAdjustment[];
  needleRecommendation: NeedleRecommendation;
  adjustmentSummary: string | null;
  warnings: string[];
};

export type AppliedGaugeSummary = {
  calculationId: number;
  adjustedCastOnStitches: number | null;
  adjustmentSummary: string | null;
  hasAdjustment: boolean;
} | null;

export type CalculateBody = {
  projectId: number;
  patternGauge?: GaugeInput;
  myGauge: GaugeInput;
  selectedSizeLabel: string;
  targetMeasurements?: Record<string, number>;
};

export const gaugeApi = {
  defaults: (projectId: number) => apiFetch<GaugeDefaults>(`/projects/${projectId}/gauge-defaults`),
  calculate: (body: CalculateBody) => apiFetch<CalculationResult>("/gauge/calculations", { method: "POST", body }),
  apply: (calculationId: number) =>
    apiFetch<void>(`/gauge/calculations/${calculationId}/apply`, { method: "POST" }),
  applied: (projectId: number) => apiFetch<AppliedGaugeSummary>(`/projects/${projectId}/gauge-calculation`),
};
