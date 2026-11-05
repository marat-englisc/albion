import { Bot, InlineKeyboard } from 'grammy';
import { autoRetry } from '@grammyjs/auto-retry';
import type { PrismaClient } from '../generated/prisma/client.js';
import type { BotContext } from './context.js';
import { config } from '../config.js';
import { Screens } from './screens.js';
import { DECKS, LEVELS } from '../content/types.js';
import { labels } from './keyboards.js';
import { cardText } from './cards.js';
import { strings, UserError } from '../utils.js';
import type { Grade } from 'ts-fsrs';

export function createBot(botToken: string, db: PrismaClient) {
  const bot = new Bot<BotContext>(botToken);
  bot.api.config.use(autoRetry({ maxRetryAttempts: 3, maxDelaySeconds: 10 }));
  const screens = new Screens(db);

  bot.use(async (ctx, next) => {
    if (!ctx.from || ctx.chat?.type !== 'private') {
      if (ctx.message) await ctx.reply('Для занятий напишите мне в личные сообщения.');
      return;
    }
    if (config.allowedUsers.size && !config.allowedUsers.has(String(ctx.from.id))) {
      if (ctx.callbackQuery) await ctx.answerCallbackQuery({ text: 'Доступ ограничен владельцем бота.' });
      else await ctx.reply('Доступ ограничен владельцем бота.');
      return;
    }
    ctx.user = await db.user.upsert({ where: { telegramId: String(ctx.from.id) },
      create: { telegramId: String(ctx.from.id), firstName: ctx.from.first_name, timezone: config.defaultTimezone, newCardsPerDay: config.defaultNewCards },
      update: { firstName: ctx.from.first_name } });
    const text = ctx.message?.text;
    if (text && (text.startsWith('/') || Object.values(labels).includes(text))) await db.exerciseSession.deleteMany({ where: { userId: ctx.user.id } });
    try { await next(); }
    catch (error) {
      if (error instanceof UserError) { await ctx.reply(error.message); return; }
      throw error;
    }
  });

  bot.command(['start', 'menu'], (ctx) => screens.home(ctx));
  bot.command('study', (ctx) => screens.learn(ctx));
  bot.command('decks', (ctx) => screens.decks(ctx));
  bot.command('grammar', (ctx) => screens.lessons(ctx, 'all', 0, false, ctx.match.trim()));
  bot.command('practice', (ctx) => screens.practice(ctx));
  bot.command('stats', (ctx) => screens.stats(ctx));
  bot.command('settings', (ctx) => screens.settings(ctx));
  bot.command('search', (ctx) => screens.search(ctx, ctx.match.trim()));
  bot.command('help', async (ctx) => {
    await ctx.reply('Как заниматься\n\n1. Выберите набор и нажмите «Учить».\n2. Вспомните ответ, затем откройте его.\n3. Оцените, насколько легко вспомнили:\n\nСнова — забыли или ошиблись.\nТрудно — вспомнили с усилием.\nХорошо — уверенно вспомнили.\nЛегко — вспомнили сразу.\n\nБот рассчитывает следующую дату по FSRS. Не ставьте «Трудно», если забыли ответ: используйте «Снова».\n\n/undo — отменить последнюю оценку\n/search слово — словарь\n/grammar тема — поиск урока\n/practice — упражнения\n/settings — настройки\n/limit 20 — новых карточек в день\n/timezone Europe/Berlin — часовой пояс\n/cancel — закрыть текущие задания\n\nОтложенные карточки можно вернуть в настройках. Прогресс сохраняется автоматически.');
  });
  bot.command('undo', async (ctx) => { await screens.study.undo(ctx.user.id); await ctx.reply('Последняя оценка отменена.'); await screens.learn(ctx); });
  bot.command('cancel', async (ctx) => { await db.studySession.deleteMany({ where: { userId: ctx.user.id } }); await ctx.reply('Текущие задания закрыты.'); await screens.home(ctx); });
  bot.command('limit', async (ctx) => {
    if (!/^\d{1,3}$/.test(ctx.match.trim()) || Number(ctx.match) > 200) throw new UserError('Укажите число от 0 до 200: /limit 20');
    await screens.updateSettings(ctx, { newCardsPerDay: Number(ctx.match) }); await screens.settings(ctx);
  });
  bot.command('timezone', async (ctx) => {
    const timezone = ctx.match.trim();
    try { new Intl.DateTimeFormat('en', { timeZone: timezone }); } catch { throw new UserError('Укажите часовой пояс IANA: /timezone Europe/Berlin'); }
    await screens.updateSettings(ctx, { timezone }); await screens.settings(ctx);
  });
  bot.hears(labels.study, (ctx) => screens.learn(ctx));
  bot.hears(labels.decks, (ctx) => screens.decks(ctx));
  bot.hears(labels.grammar, (ctx) => screens.lessons(ctx));
  bot.hears(labels.practice, (ctx) => screens.practice(ctx));
  bot.hears(labels.stats, (ctx) => screens.stats(ctx));
  bot.hears(labels.settings, (ctx) => screens.settings(ctx));

  bot.on('callback_query:data', async (ctx) => {
    await ctx.answerCallbackQuery();
    const [action, a = '', b = '', c = ''] = ctx.callbackQuery.data.split(':');
    switch (action) {
      case 'learn': await db.exerciseSession.deleteMany({ where: { userId: ctx.user.id } }); await screens.learn(ctx); break;
      case 'decks': await screens.decks(ctx, true); break;
      case 'deck':
        if (a !== 'all' && !DECKS.some((d) => d.id === a)) throw new UserError('Набор не найден.');
        await screens.updateSettings(ctx, { activeDeck: a });
        await screens.decks(ctx, true);
        await screens.message(ctx, 'Набор выбран. Начнём?', new InlineKeyboard().text('Учить', 'learn'));
        break;
      case 'show': await screens.showCard(ctx, a); break;
      case 'rate':
        if (!/^[1-4]$/.test(b)) throw new UserError('Неизвестная оценка.');
        await screens.study.rate(ctx.user, a, Number(b) as Grade);
        await ctx.editMessageReplyMarkup({ reply_markup: new InlineKeyboard() });
        await screens.learn(ctx);
        break;
      case 'audio': {
        const session = await screens.study.current(ctx.user.id);
        if (!session || session.token !== a) throw new UserError('Откройте текущую карточку: /study.');
        if (!session.revealed && session.card.direction === 'production') throw new UserError('Сначала откройте ответ.');
        await screens.audio.send(ctx, session.card.note, session.revealed); break;
      }
      case 'suspend': await screens.study.suspend(ctx.user.id, a); await ctx.editMessageReplyMarkup({ reply_markup: new InlineKeyboard() }); await screens.learn(ctx); break;
      case 'stop':
        await db.studySession.deleteMany({ where: { userId: ctx.user.id, token: a } });
        await ctx.editMessageReplyMarkup({ reply_markup: new InlineKeyboard() }); await screens.home(ctx); break;
      case 'setting': {
        if (a === 'direction') await screens.updateSettings(ctx, { direction: ctx.user.direction === 'recognition' ? 'production' : 'recognition' });
        else if (a === 'audio') await screens.updateSettings(ctx, { autoAudio: !ctx.user.autoAudio });
        else if (a === 'new' && ['0', '10', '20', '30', '50'].includes(b)) await screens.updateSettings(ctx, { newCardsPerDay: Number(b) });
        else if (a === 'retention' && ['85', '90', '95'].includes(b)) await screens.updateSettings(ctx, { retention: Number(b) / 100 });
        else if (a === 'level' && (b === 'all' || LEVELS.includes(b as typeof LEVELS[number]))) await screens.updateSettings(ctx, { level: b });
        else throw new UserError('Настройка не найдена.');
        await screens.settings(ctx, true); break;
      }
      case 'unsuspend': await db.userCard.updateMany({ where: { userId: ctx.user.id, suspended: true }, data: { suspended: false, version: { increment: 1 } } }); await ctx.reply('Отложенные карточки возвращены.'); break;
      case 'lessons': await db.exerciseSession.deleteMany({ where: { userId: ctx.user.id } }); await screens.lessons(ctx, a, Number(b) || 0, true); break;
      case 'lesson': await db.exerciseSession.deleteMany({ where: { userId: ctx.user.id } }); await screens.lesson(ctx, Number(a), b === 'resume' ? 'resume' : Number(b) || 0, c, true); break;
      case 'complete': {
        const lesson = await db.lesson.findUnique({ where: { id: Number(a) } });
        if (!lesson?.active) throw new UserError('Урок не найден.');
        const key = { userId: ctx.user.id, lessonId: lesson.id };
        const previous = await db.lessonProgress.findUnique({ where: { userId_lessonId: key } });
        await db.lessonProgress.upsert({ where: { userId_lessonId: key }, create: { ...key, completedAt: new Date() }, update: { completedAt: previous?.completedAt ? null : new Date() } });
        await screens.lesson(ctx, lesson.id, 'resume', 'main', true); break;
      }
      case 'practice': await screens.practice(ctx, a === 'all' ? undefined : Number(a) || undefined); break;
      case 'answer': {
        const session = await db.exerciseSession.findUnique({ where: { userId: ctx.user.id }, include: { exercise: true } });
        if (!session || session.token !== a || !/^\d+$/.test(b)) throw new UserError('Задание закрыто. Нажмите /practice.');
        const answer = strings(session.exercise.options)[Number(b)];
        if (answer === undefined) throw new UserError('Вариант не найден.');
        await screens.exerciseResult(ctx, a, answer); break;
      }
      case 'solution': await screens.exerciseResult(ctx, a, '', true); break;
      case 'practice-stop': await db.exerciseSession.deleteMany({ where: { userId: ctx.user.id, token: a } }); await ctx.editMessageReplyMarkup({ reply_markup: new InlineKeyboard() }); await screens.home(ctx); break;
      case 'dictionary': {
        const note = await db.note.findUnique({ where: { id: Number(a) } });
        if (!note?.active) throw new UserError('Карточка не найдена.');
        await screens.message(ctx, cardText(note, { direction: 'recognition' }, true), new InlineKeyboard().text('🔊 Озвучить', `dictionary-audio:${note.id}`)); break;
      }
      case 'dictionary-audio': {
        const note = await db.note.findUnique({ where: { id: Number(a) } });
        if (!note?.active) throw new UserError('Карточка не найдена.');
        await screens.audio.send(ctx, note, true); break;
      }
      default: throw new UserError('Кнопка устарела. Нажмите /menu.');
    }
  });

  bot.on('message:text', async (ctx) => {
    const session = await db.exerciseSession.findUnique({ where: { userId: ctx.user.id }, include: { exercise: true } });
    if (session?.exercise.kind === 'input') { await screens.exerciseResult(ctx, session.token, ctx.message.text); return; }
    await ctx.reply('Выберите занятие в меню или откройте /help. Для поиска слова: /search слово.');
  });
  return bot;
}
