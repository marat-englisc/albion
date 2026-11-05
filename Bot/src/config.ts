import "dotenv/config";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { z } from "zod";

// Both src/config.ts and dist/config.js are one directory below the project root.
export const botRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const timezone = z.string().refine((value) => {
  try {
    new Intl.DateTimeFormat("en", { timeZone: value });
    return true;
  } catch {
    return false;
  }
}, "Укажите часовой пояс IANA, например Europe/Berlin");

const env = z
  .object({
    DATABASE_URL: z
      .string()
      .startsWith("file:")
      .default("file:./data/albion.db"),
    CONTENT_ROOT: z.string().default("./content"),
    DEFAULT_TIMEZONE: timezone.default("Europe/Berlin"),
    DEFAULT_NEW_CARDS: z.coerce.number().int().min(0).max(200).default(20),
    ALLOWED_USER_IDS: z.string().default(""),
    LOG_LEVEL: z
      .enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"])
      .default("info"),
  })
  .parse(process.env);

export const config = {
  databaseUrl: env.DATABASE_URL,
  contentRoot: path.resolve(botRoot, env.CONTENT_ROOT),
  defaultTimezone: env.DEFAULT_TIMEZONE,
  defaultNewCards: env.DEFAULT_NEW_CARDS,
  logLevel: env.LOG_LEVEL,
  allowedUsers: new Set(
    env.ALLOWED_USER_IDS.split(",")
      .map((s) => s.trim())
      .filter(Boolean),
  ),
};

export function getBotToken(): string {
  return z
    .string()
    .regex(/^\d+:[\w-]{20,}$/, "Добавьте BOT_TOKEN из @BotFather в Bot/.env")
    .parse(process.env.BOT_TOKEN);
}
