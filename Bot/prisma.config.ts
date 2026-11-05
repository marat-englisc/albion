import "dotenv/config";
import { defineConfig } from "prisma/config";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.dirname(fileURLToPath(import.meta.url));
const databaseUrl = `file:${path.resolve(root, (process.env.DATABASE_URL || "file:./data/albion.db").slice(5)).replaceAll("\\", "/")}`;

export default defineConfig({
  schema: "prisma/schema.prisma",
  migrations: { path: "prisma/migrations" },
  datasource: { url: databaseUrl },
});
