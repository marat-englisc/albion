import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { testDatabase, sampleContent, sampleNote } from './helpers.js';
import { importContent } from '../src/content/import.js';
import { StudyService } from '../src/services/study.js';
import { localDay } from '../src/utils.js';
import type { User } from '../src/generated/prisma/client.js';

describe('persistent FSRS study', () => {
  let fixture: Awaited<ReturnType<typeof testDatabase>>;
  let study: StudyService;
  let user: User;
  const now = new Date('2026-11-05T20:00:00Z');
  beforeEach(async () => {
    fixture = await testDatabase(); await importContent(fixture.db, sampleContent());
    user = await fixture.db.user.create({ data: { telegramId: '1', firstName: 'Marat', newCardsPerDay: 1 } });
    study = new StudyService(fixture.db);
  });
  afterEach(async () => { await fixture.close(); });

  it('requires revealing, accepts one rating, and resumes across service instances', async () => {
    const session = (await study.next(user, now))!;
    await expect(study.rate(user, session.token, 3, now)).rejects.toThrow('Сначала');
    expect((await new StudyService(fixture.db).next(user, now))?.token).toBe(session.token);
    await study.reveal(user.id, session.token);
    const result = await study.rate(user, session.token, 3, now);
    expect(result.reps).toBe(1); expect(result.state).toBe(1); expect(result.due.getTime()).toBeGreaterThan(now.getTime());
    await expect(study.rate(user, session.token, 3, now)).rejects.toThrow();
    expect(await fixture.db.reviewLog.count()).toBe(1);
  });

  it('limits new cards but always allows due learning/review cards', async () => {
    const session = (await study.next(user, now))!;
    await study.reveal(user.id, session.token); await study.rate(user, session.token, 1, now);
    expect(await study.next(user, now)).toBeNull();
    const later = new Date(now.getTime() + 61_000);
    expect((await study.next(user, later))?.cardId).toBe(session.cardId);
    expect((await study.summary(user, later)).newRemaining).toBe(0);
  });

  it('graduates a learned card and records a later lapse as relearning', async () => {
    const first = (await study.next(user, now))!;
    await study.reveal(user.id, first.token);
    const learning = await study.rate(user, first.token, 3, now);
    const second = (await study.next(user, learning.due))!;
    await study.reveal(user.id, second.token);
    const review = await study.rate(user, second.token, 3, learning.due);
    expect(review.state).toBe(2);
    const third = (await study.next({ ...user, newCardsPerDay: 0 }, review.due))!;
    await study.reveal(user.id, third.token);
    const lapse = await study.rate(user, third.token, 1, review.due);
    expect(lapse.state).toBe(3); expect(lapse.lapses).toBe(1);
    expect(await fixture.db.reviewLog.count()).toBe(3);
  });

  it('undo restores the exact scheduling fields and frees the new-card allowance', async () => {
    const session = (await study.next(user, now))!;
    await study.reveal(user.id, session.token); await study.rate(user, session.token, 4, now);
    await study.undo(user.id);
    const restored = (await study.current(user.id))!;
    expect(restored.card.state).toBe(0); expect(restored.card.reps).toBe(0); expect(restored.card.introducedDay).toBeNull();
    expect(restored.token).not.toBe(session.token); expect(restored.card.due).toEqual(session.card.due);
    expect((await study.summary(user, now)).newRemaining).toBe(1);
    expect(await fixture.db.reviewLog.count({ where: { undone: false } })).toBe(0);
  });

  it('does not let one user reveal or rate another user’s card', async () => {
    const other = await fixture.db.user.create({ data: { telegramId: '2', firstName: 'Other' } });
    const session = (await study.next(user, now))!;
    await expect(study.reveal(other.id, session.token)).rejects.toThrow();
    await expect(study.rate(other, session.token, 3, now)).rejects.toThrow();
  });

  it('keeps direction progress independent and applies a shared daily limit', async () => {
    const session = (await study.next(user, now))!;
    await study.reveal(user.id, session.token); await study.rate(user, session.token, 4, now);
    const reverse = { ...user, direction: 'production' };
    expect(await study.next(reverse, now)).toBeNull();
    const nextDay = new Date('2026-11-06T20:00:00Z');
    const production = (await study.next(reverse, nextDay))!;
    expect(production.card.direction).toBe('production'); expect(production.card.reps).toBe(0);
    expect(production.cardId).not.toBe(session.cardId);
  });

  it('reimport preserves scheduling, archives removed notes, and revives them', async () => {
    const session = (await study.next(user, now))!;
    await study.reveal(user.id, session.token); await study.rate(user, session.token, 4, now);
    const before = await fixture.db.userCard.findUnique({ where: { id: session.cardId } });
    const content = sampleContent(); content.notes[0]!.translation = 'яблоко, плод';
    await importContent(fixture.db, content);
    expect(await fixture.db.userCard.findUnique({ where: { id: session.cardId } })).toEqual(before);
    content.notes = [sampleNote({ sourceKey: 'book', term: 'book' })];
    await importContent(fixture.db, content);
    expect((await fixture.db.note.findUnique({ where: { sourceKey: 'apple' } }))?.active).toBe(false);
    await importContent(fixture.db, sampleContent());
    expect((await fixture.db.note.findUnique({ where: { sourceKey: 'apple' } }))?.active).toBe(true);
    expect(await fixture.db.reviewLog.count()).toBe(1);
  });

  it('suspends only the current user’s direction and rejects stale tokens', async () => {
    const session = (await study.next(user, now))!;
    await study.suspend(user.id, session.token);
    await expect(study.reveal(user.id, session.token)).rejects.toThrow();
    const next = (await study.next(user, now))!;
    expect(next.cardId).not.toBe(session.cardId);
  });

  it('calculates day boundaries in the user’s timezone', () => {
    expect(localDay(new Date('2026-11-05T23:30:00Z'), 'Europe/Berlin')).toBe('2026-11-06');
    expect(localDay(new Date('2026-03-29T22:30:00Z'), 'Europe/Berlin')).toBe('2026-03-30');
  });
});
