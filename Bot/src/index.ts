import pino from "pino";
import { GrammyError } from "grammy";
import { config, getBotToken } from "./config.js";
import { createDatabase, configureDatabase } from "./db.js";
import { createBot } from "./bot/create-bot.js";

const logger = pino({ level: config.logLevel });
const botToken = getBotToken();
const db = createDatabase();

await configureDatabase(db);

if (!(await db.note.count({ where: { active: true } }))) {
  await db.$disconnect();
  throw new Error(
    "Материалы не импортированы. Сначала выполните npm run setup.",
  );
}

const bot = createBot(botToken, db);

bot.catch(async ({ error, ctx }) => {
  // Never log API URLs, bot tokens, message bodies or complete HTTP error objects.
  logger.error(
    {
      kind: error instanceof Error ? error.name : "Unknown",
      description: error instanceof GrammyError ? error.description : undefined,
      updateId: ctx.update.update_id,
    },
    "Не удалось обработать обновление",
  );
  try {
    await ctx.reply(
      "Не удалось выполнить действие. Ваш прогресс сохранён. Попробуйте /menu.",
    );
  } catch {
    /* Telegram may be unavailable. */
  }
});

let stopping = false;

async function stop() {
  if (stopping) return;
  stopping = true;
  if (bot.isRunning()) await bot.stop();
}

process.once("SIGINT", () => void stop());
process.once("SIGTERM", () => void stop());

try {
  await bot.init();
  await bot.api.setMyCommands([
    { command: "menu", description: "Главное меню" },
    { command: "study", description: "Учить карточки" },
    { command: "decks", description: "Выбрать набор" },
    { command: "grammar", description: "Уроки грамматики" },
    { command: "practice", description: "Грамматические упражнения" },
    { command: "stats", description: "Мой прогресс" },
    { command: "settings", description: "Настройки обучения" },
    { command: "undo", description: "Отменить последнюю оценку" },
    { command: "search", description: "Найти слово" },
    { command: "help", description: "Как заниматься" },
  ]);
  logger.info({ username: bot.botInfo.username }, "Albion запущен");
  // Sequential long polling suits SQLite and prevents one user's updates from overtaking each other.
  if (!stopping)
    await bot.start({
      allowed_updates: ["message", "callback_query"],
      drop_pending_updates: false,
    });
} finally {
  await db.$disconnect();
}
