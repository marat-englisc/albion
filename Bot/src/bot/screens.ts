import { InlineKeyboard, GrammyError } from 'grammy';
import type { PrismaClient, User } from '../generated/prisma/client.js';
import type { BotContext } from './context.js';
import { StudyService } from '../services/study.js';
import { ExerciseService } from '../services/exercises.js';
import { AudioService } from '../services/audio.js';
import { statistics } from '../services/stats.js';
import { cardText, cardKeyboard } from './cards.js';
import { deckKeyboard, mainKeyboard } from './keyboards.js';
import { DECKS, LEVELS } from '../content/types.js';
import { lessonSections, markdownPages } from '../content/markdown.js';
import { escapeHtml as h, strings, dateTime, UserError } from '../utils.js';

export class Screens {
  readonly study: StudyService;
  readonly exercises: ExerciseService;
  readonly audio: AudioService;
  constructor(readonly db: PrismaClient) {
    this.study = new StudyService(db); this.exercises = new ExerciseService(db); this.audio = new AudioService(db);
  }

  async message(ctx: BotContext, text: string, keyboard?: InlineKeyboard, edit = false) {
    const options = { parse_mode: 'HTML' as const, reply_markup: keyboard, link_preview_options: { is_disabled: true } };
    if (edit && ctx.callbackQuery?.message) {
      try { await ctx.editMessageText(text, options); return; }
      catch (error) { if (error instanceof GrammyError && /message is not modified/i.test(error.description)) return; throw error; }
    }
    await ctx.reply(text, options);
  }

  async home(ctx: BotContext) {
    const summary = await this.study.summary(ctx.user);
    await ctx.reply(`<b>Albion · ваш английский</b>\n\nПривет, ${h(ctx.user.firstName)}!\nК повторению сейчас: <b>${summary.due}</b>.\nНовых карточек на сегодня: до <b>${summary.newRemaining}</b>.\n\nВыберите занятие в меню. /help — как учиться.`,
      { parse_mode: 'HTML', reply_markup: mainKeyboard });
  }

  async learn(ctx: BotContext) {
    const session = await this.study.next(ctx.user);
    if (!session) {
      const summary = await this.study.summary(ctx.user);
      await this.message(ctx, `<b>На сейчас всё!</b>\n\nНет доступных карточек для выбранного набора и уровня.${summary.newRemaining === 0 ? '\nЛимит новых карточек на сегодня исчерпан.' : ''}${summary.upcoming ? `\nБлижайшее повторение: ${h(dateTime(summary.upcoming, ctx.user.timezone))}.` : ''}\n\nМожно выбрать другой набор, почитать грамматику или вернуться позже.`,
        new InlineKeyboard().text('Проверить снова', 'learn').row().text('Выбрать набор', 'decks').text('Грамматика', 'lessons:all:0'));
      return;
    }
    await this.message(ctx, `<b>${h(session.card.note.deck.title)}</b>\n${cardText(session.card.note, session.card, session.revealed)}`,
      cardKeyboard(session.card, ctx.user.retention, session.token, session.revealed));
    if (ctx.user.autoAudio && (session.revealed || ctx.user.direction === 'recognition')) {
      try { await this.audio.send(ctx, session.card.note, session.revealed); }
      catch (error) { await this.message(ctx, error instanceof UserError ? error.message : 'Не удалось отправить озвучку. Попробуйте кнопку 🔊.'); }
    }
  }

  async showCard(ctx: BotContext, sessionToken: string) {
    const session = await this.study.reveal(ctx.user.id, sessionToken);
    if (!session) throw new UserError('Карточка закрыта. Нажмите /study.');
    await this.message(ctx, `<b>${h(session.card.note.deck.title)}</b>\n${cardText(session.card.note, session.card, true)}`,
      cardKeyboard(session.card, ctx.user.retention, session.token, true), true);
    if (ctx.user.autoAudio && ctx.user.direction === 'production') {
      try { await this.audio.send(ctx, session.card.note, true); }
      catch { await this.message(ctx, 'Не удалось отправить озвучку. Попробуйте кнопку 🔊.'); }
    }
  }

  async decks(ctx: BotContext, edit = false) {
    const counts = await this.db.note.groupBy({ by: ['deckId'], where: { active: true }, _count: true });
    await this.message(ctx, `<b>Ваши наборы</b>\n\n${DECKS.map((d) => `${h(d.title)} — ${counts.find((c) => c.deckId === d.id)?._count ?? 0}`).join('\n')}\n\nВыбор набора меняет очередь повторений.`, deckKeyboard(ctx.user.activeDeck), edit);
  }

  async settings(ctx: BotContext, edit = false) {
    const user = ctx.user;
    const deck = DECKS.find((d) => d.id === user.activeDeck)?.title ?? 'Все наборы';
    const keyboard = new InlineKeyboard().text(`Направление: ${user.direction === 'recognition' ? 'EN → RU' : 'RU → EN'}`, 'setting:direction').row()
      .text(`Озвучка: ${user.autoAudio ? 'вкл' : 'по кнопке'}`, 'setting:audio').row();
    for (const n of [0, 10, 20, 30, 50]) keyboard.text(`${user.newCardsPerDay === n ? '✓ ' : ''}${n} новых`, `setting:new:${n}`);
    keyboard.row();
    for (const n of [85, 90, 95]) keyboard.text(`${Math.round(user.retention * 100) === n ? '✓ ' : ''}${n}%`, `setting:retention:${n}`);
    keyboard.row().text(`${user.level === 'all' ? '✓ ' : ''}Все уровни`, 'setting:level:all').row();
    for (const [i, level] of LEVELS.entries()) { keyboard.text(`${user.level === level ? '✓ ' : ''}${level}`, `setting:level:${level}`); if (i === 2) keyboard.row(); }
    keyboard.row().text('Вернуть отложенные карточки', 'unsuspend');
    await this.message(ctx, `<b>Настройки</b>\n\nНабор: ${h(deck)}\nУровень: ${h(user.level === 'all' ? 'все' : user.level)}\nЧасовой пояс: ${h(user.timezone)}\n\nЛимит новых карточек общий для всех наборов и направлений. Просроченные повторения доступны без лимита.\n\n85 / 90 / 95% — целевая вероятность вспомнить ответ. Чем она выше, тем чаще повторения.\nКарточки без CEFR доступны при любом уровне.\n\nДля своего лимита: <code>/limit 15</code>\nДля часового пояса: <code>/timezone Europe/Berlin</code>`, keyboard, edit);
  }

  async updateSettings(ctx: BotContext, data: Partial<Pick<User, 'activeDeck' | 'level' | 'direction' | 'autoAudio' | 'newCardsPerDay' | 'retention' | 'timezone'>>) {
    ctx.user = await this.db.user.update({ where: { id: ctx.user.id }, data });
    await this.db.studySession.deleteMany({ where: { userId: ctx.user.id } });
  }

  async stats(ctx: BotContext) {
    const s = await statistics(this.db, ctx.user);
    const summary = await this.study.summary(ctx.user);
    await this.message(ctx, `<b>Ваш прогресс</b>\n\nСегодня: <b>${s.today}</b> повторений\nВспомнили: ${s.recall === null ? 'пока нет ответов' : `${s.recall}%`}\nСерия занятий: <b>${s.streak}</b> дн.\nВсего повторений: ${s.total}\n\nНа изучении: ${s.learning}\nНа долгосрочном повторении: ${s.learned}\nК повторению в выбранном наборе: ${summary.due}\n\nПройдено уроков: ${s.completed}\nУпражнения: ${s.correct} правильных из ${s.attempts}\n\nEN → RU и RU → EN учитываются отдельно.`, new InlineKeyboard().text('Продолжить', 'learn'));
  }

  async lessons(ctx: BotContext, level = 'all', page = 0, edit = false, search = '') {
    const baseWhere = { active: true, ...(level === 'ref' ? { id: { gte: 1000 } } : LEVELS.includes(level as typeof LEVELS[number]) ? { level } : {}) };
    const needle = search.toLocaleLowerCase('ru');
    const matches = search ? (await this.db.lesson.findMany({ where: baseWhere, select: { id: true, title: true, keywords: true } }))
      .filter((l) => `${l.title}\n${l.keywords}`.toLocaleLowerCase('ru').includes(needle)).map((l) => l.id) : [];
    const where = { ...baseWhere, ...(search ? { id: { in: matches } } : {}) };
    const total = await this.db.lesson.count({ where });
    const pages = Math.max(1, Math.ceil(total / 8));
    page = Math.max(0, Math.min(page, pages - 1));
    const lessons = await this.db.lesson.findMany({ where, orderBy: { id: 'asc' }, skip: page * 8, take: 8,
      include: { progress: { where: { userId: ctx.user.id } } } });
    const keyboard = new InlineKeyboard();
    for (const lesson of lessons) keyboard.text(`${lesson.progress[0]?.completedAt ? '✓ ' : ''}${lesson.id < 1000 ? `${lesson.id}. ` : ''}${lesson.title}`.slice(0, 105), `lesson:${lesson.id}:resume:main`).row();
    if (page > 0 && !search) keyboard.text('←', `lessons:${level}:${page - 1}`);
    if (page < pages - 1 && !search) keyboard.text('→', `lessons:${level}:${page + 1}`);
    keyboard.row().text('Все', 'lessons:all:0');
    for (const [i, l] of LEVELS.entries()) { keyboard.text(l, `lessons:${l}:0`); if (i === 2) keyboard.row(); }
    keyboard.row().text('Справочники', 'lessons:ref:0');
    await this.message(ctx, `<b>Грамматика</b>\n\n${search ? `Поиск: ${h(search)}\n` : ''}Уроков: ${total} · страница ${page + 1}/${pages}\n\nВыберите тему. Можно искать: <code>/grammar Present Perfect</code>\nТекстовый поиск показывает первые 8 совпадений.`, keyboard, edit);
  }

  async lesson(ctx: BotContext, id: number, page: number | 'resume', mode = 'main', edit = false) {
    const lesson = await this.db.lesson.findUnique({ where: { id } });
    if (!lesson?.active) throw new UserError('Урок не найден. Нажмите /grammar.');
    const progress = await this.db.lessonProgress.findUnique({ where: { userId_lessonId: { userId: ctx.user.id, lessonId: id } } });
    const sections = lessonSections(lesson.markdown);
    const pages = markdownPages(mode === 'answers' ? sections.answers || 'Ответов к этому уроку нет.' : sections.main);
    const index = Math.max(0, Math.min(page === 'resume' ? progress?.lastPage ?? 0 : page, pages.length - 1));
    if (mode === 'main') await this.db.lessonProgress.upsert({ where: { userId_lessonId: { userId: ctx.user.id, lessonId: id } },
      create: { userId: ctx.user.id, lessonId: id, lastPage: index }, update: { lastPage: index } });
    const keyboard = new InlineKeyboard();
    if (index > 0) keyboard.text('← Назад', `lesson:${id}:${index - 1}:${mode}`);
    if (index < pages.length - 1) keyboard.text('Далее →', `lesson:${id}:${index + 1}:${mode}`);
    keyboard.row();
    if (mode === 'main' && sections.answers) keyboard.text('Ответы к уроку', `lesson:${id}:0:answers`).row();
    if (mode === 'answers') keyboard.text('Вернуться к уроку', `lesson:${id}:resume:main`).row();
    const count = await this.db.exercise.count({ where: { lessonId: id, active: true } });
    if (count) keyboard.text(`Практика · ${count}`, `practice:${id}`).row();
    keyboard.text(progress?.completedAt ? '✓ Пройдено · снять отметку' : 'Отметить пройденным', `complete:${id}`).row();
    const adjacent = await this.db.lesson.findFirst({ where: { active: true, id: { gt: id } }, orderBy: { id: 'asc' } });
    if (adjacent) keyboard.text('Следующий урок', `lesson:${adjacent.id}:resume:main`).row();
    keyboard.text('Оглавление', `lessons:${lesson.level || 'all'}:0`);
    await this.message(ctx, `<b>${id < 1000 ? `${id}. ` : ''}${h(lesson.title)}</b>\n${h(lesson.level)} · ${index + 1}/${pages.length}${mode === 'answers' ? ' · Ответы' : ''}\n\n${pages[index]}`, keyboard, edit);
  }

  async practice(ctx: BotContext, lessonId?: number) {
    const session = await this.exercises.next(ctx.user.id, lessonId);
    if (!session) { await this.message(ctx, 'Для этой темы пока нет упражнений.'); return; }
    const exercise = session.exercise;
    const options = strings(exercise.options);
    const keyboard = new InlineKeyboard();
    options.forEach((option, i) => keyboard.text(`${i + 1}. ${option}`.slice(0, 100), `answer:${session.token}:${i}`).row());
    keyboard.text('Показать решение', `solution:${session.token}`).row().text('Завершить практику', `practice-stop:${session.token}`);
    await this.message(ctx, `<b>${h(exercise.topic)}</b> · ${h(exercise.level)}\n\n${h(exercise.prompt)}${exercise.kind === 'input' ? '\n\nНапишите только ответ или пропущенную часть. Для нескольких пропусков разделяйте части знаком /.' : '\n\nВыберите вариант.'}`, keyboard);
  }

  async exerciseResult(ctx: BotContext, sessionToken: string, answer: string, reveal = false) {
    const result = await this.exercises.answer(ctx.user.id, sessionToken, answer);
    if (ctx.callbackQuery?.message) await ctx.editMessageReplyMarkup({ reply_markup: new InlineKeyboard() });
    const keyboard = new InlineKeyboard().text('Следующее задание', `practice:${result.lessonId ?? 'all'}`).row();
    if (result.lessonId) keyboard.text('Открыть урок', `lesson:${result.lessonId}:resume:main`);
    await this.message(ctx, `<b>${reveal ? 'Решение · засчитано как ошибка' : result.correct ? 'Верно!' : 'Пока неверно'}</b>\n\n${!reveal ? `Ваш ответ: ${h(answer)}\n` : ''}Правильный ответ: <b>${h(result.expected.join(' / '))}</b>\n\n${h(result.exercise.explanation)}`, keyboard);
  }

  async search(ctx: BotContext, query: string) {
    if (!query.trim()) { await this.message(ctx, 'Например: /search take off'); return; }
    // SQLite's built-in case folding covers ASCII only. Fold in JS for Cyrillic search.
    const needle = query.toLocaleLowerCase('ru');
    const candidates = await this.db.note.findMany({ where: { active: true }, select: { id: true, term: true, translation: true }, orderBy: { term: 'asc' } });
    const notes = candidates.filter((n) => `${n.term}\n${n.translation}`.toLocaleLowerCase('ru').includes(needle)).slice(0, 8);
    const keyboard = new InlineKeyboard();
    for (const note of notes) keyboard.text(note.term, `dictionary:${note.id}`).row();
    await this.message(ctx, `<b>Поиск: ${h(query)}</b>\n\n${notes.length ? 'Первые совпадения. Выберите карточку, чтобы посмотреть значение.' : 'Совпадений нет.'}`, keyboard);
  }
}
