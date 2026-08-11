import { apiFetch } from "@/lib/api";

export type SellerApplicationBody = {
  brandName: string;
  businessType?: "INDIVIDUAL" | "BUSINESS";
  businessNo?: string;
  representativeName?: string;
  settlementBank?: string;
  settlementAccount?: string;
  termsVersion: string;
};

export const sellerApi = {
  apply: (body: SellerApplicationBody) =>
    apiFetch<{ id: number; status: string }>("/seller-applications", { method: "POST", body }),
};
