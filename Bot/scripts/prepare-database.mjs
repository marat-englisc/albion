import "dotenv/config";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { mkdirSync, openSync, closeSync } from "node:fs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

const url = process.env.DATABASE_URL || "file:./data/albion.db";
if (!url.startsWith("file:"))
  throw new Error("DATABASE_URL должен начинаться с file:");

const filename = path.resolve(root, url.slice(5));
mkdirSync(path.dirname(filename), { recursive: true });
// Pre-create an empty file for Prisma's SQLite schema engine; append mode never truncates data.
closeSync(openSync(filename, "a"));
