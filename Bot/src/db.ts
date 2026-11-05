import path from "node:path";
import { mkdirSync } from "node:fs";
import { PrismaBetterSqlite3 } from "@prisma/adapter-better-sqlite3";
import { PrismaClient } from "./generated/prisma/client.js";
import { botRoot, config } from "./config.js";

export function createDatabase(url = config.databaseUrl): PrismaClient {
  const filename = path.resolve(botRoot, url.slice("file:".length));
  mkdirSync(path.dirname(filename), { recursive: true });
  return new PrismaClient({
    adapter: new PrismaBetterSqlite3({
      url: `file:${filename.replaceAll("\\", "/")}`,
    }),
  });
}

export async function configureDatabase(db: PrismaClient): Promise<void> {
  await db.$queryRawUnsafe("PRAGMA journal_mode = WAL");
  await db.$queryRawUnsafe("PRAGMA busy_timeout = 5000");
  await db.$executeRawUnsafe("PRAGMA foreign_keys = ON");
}
