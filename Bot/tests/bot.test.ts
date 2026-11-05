import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { testDatabase, sampleContent } from './helpers.js';
import { importContent } from '../src/content/import.js';
import { createBot } from '../src/bot/create-bot.js';
import type { Update } from 'grammy/types';

describe('Telegram flows with a mocked API', () => {
  let fixture: Awaited<ReturnType<typeof testDatabase>>;
  let bot: ReturnType<typeof createBot>;
  let calls: { method: string; payload: Record<string, unknown> }[];
  let sequence: number;
  beforeEach(async () => {
    fixture = await testDatabase(); await importContent(fixture.db, sampleContent()); sequence = 1; calls = [];
    bot = createBot('123456:ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghi', fixture.db);
    bot.api.config.use(async (_prev, method, payload) => {
      calls.push({ method, payload: payload as Record<string, unknown> });
      if (method === 'getMe') return { ok: true, result: { id: 123456, is_bot: true, first_name: 'Albion', username: 'albion_test_bot' } } as never;
      return { ok: true, result: { message_id: sequence++, date: 0, chat: { id: 42, type: 'private' }, text: 'ok' } } as never;
    });
    await bot.init();
  });
  afterEach(async () => { await fixture.close(); });
  const from = { id: 42, is_bot: false, first_name: 'Marat' };
  async function message(text: string) {
    await bot.handleUpdate({ update_id: sequence++, message: { message_id: sequence++, date: 0, from, chat: { id: 42, type: 'private' }, text,
      ...(text.startsWith('/') ? { entities: [{ type: 'bot_command', offset: 0, length: text.split(' ')[0]!.length }] } : {}) } } as Update);
  }
  async function callback(data: string) {
    await bot.handleUpdate({ update_id: sequence++, callback_query: { id: String(sequence++), from, chat_instance: '1', data,
      message: { message_id: 10, date: 0, chat: { id: 42, type: 'private' }, from: { id: 123456, is_bot: true, first_name: 'Albion' }, text: 'card' } } } as Update);
  }
  it('walks through start, reveal, rating and rejects duplicate ratings', async () => {
    await message('/start'); await message('/study');
    const session = (await fixture.db.studySession.findFirst())!;
    expect(calls.some((c) => c.method === 'sendMessage' && String(c.payload.text).includes('яблоко'))).toBe(false);
    await callback(`show:${session.token}`);
    expect(calls.some((c) => c.method === 'editMessageText' && String(c.payload.text).includes('яблоко'))).toBe(true);
    await callback(`rate:${session.token}:3`); await callback(`rate:${session.token}:3`);
    expect(await fixture.db.reviewLog.count()).toBe(1);
  });
  it('persists lesson progress, completion and exercise results', async () => {
    await message('/grammar'); await callback('lesson:1:0:main');
    expect(calls.some((c) => c.method === 'editMessageText' && String(c.payload.text).includes('Answer.'))).toBe(false);
    await callback('complete:1'); expect((await fixture.db.lessonProgress.findFirst())?.completedAt).not.toBeNull();
    await callback('practice:1'); const session = (await fixture.db.exerciseSession.findFirst())!;
    await callback(`answer:${session.token}:0`);
    expect((await fixture.db.exerciseAttempt.findFirst())?.correct).toBe(true);
    await callback('practice:1'); await message('Have/eaten');
    expect(await fixture.db.exerciseAttempt.count({ where: { correct: true } })).toBe(2);
  });
  it('does not reveal the English prompt via audio in production mode', async () => {
    await message('/settings'); await callback('setting:direction'); await message('/study');
    const session = (await fixture.db.studySession.findFirst())!;
    await callback(`audio:${session.token}`);
    expect(calls.some((c) => c.method === 'sendAudio')).toBe(false);
    expect(calls.at(-1)?.payload.text).toBe('Сначала откройте ответ.');
  });
});
