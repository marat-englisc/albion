import { randomBytes } from "node:crypto";

export const token = () => randomBytes(6).toString("hex");

export const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

export function localDay(date: Date, timezone: string): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: timezone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

export function dateTime(date: Date, timezone: string): string {
  return new Intl.DateTimeFormat("ru", {
    timeZone: timezone,
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function interval(due: Date, now: Date): string {
  const minutes = Math.max(
    1,
    Math.round((due.getTime() - now.getTime()) / 60_000),
  );
  if (minutes < 60) return `${minutes} мин`;
  if (minutes < 1440) return `${Math.round(minutes / 60)} ч`;
  const days = Math.round(minutes / 1440);
  return days < 365 ? `${days} дн` : `${(days / 365).toFixed(1)} г`;
}

export function strings(value: unknown): string[] {
  return Array.isArray(value)
    ? value.filter((x): x is string => typeof x === "string")
    : [];
}

export class UserError extends Error {}
