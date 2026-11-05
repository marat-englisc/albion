import { beforeAll, describe, expect, it } from 'vitest';
import { stat } from 'node:fs/promises';
import path from 'node:path';
import { loadContent } from '../src/content/load.js';
import { config } from '../src/config.js';
import { lessonSections, markdownPages } from '../src/content/markdown.js';
import { cardText } from '../src/bot/cards.js';
import type { Content } from '../src/content/types.js';
import type { Note, UserCard } from '../src/generated/prisma/client.js';
import { normalizeAnswer } from '../src/services/exercises.js';

describe('project learning materials', () => {
  let content: Content;
  beforeAll(async () => { content = await loadContent(config.contentRoot); }, 30_000);

  it('loads all five collections, 144 lessons, five references and linked exercises', () => {
    expect(new Set(content.notes.map((n) => n.deckId)).size).toBe(5);
    expect(content.notes.length).toBeGreaterThan(7000);
    expect(content.lessons.filter((l) => l.id < 1000).length).toBeGreaterThanOrEqual(144);
    expect(content.lessons.filter((l) => l.id >= 1000).length).toBeGreaterThanOrEqual(5);
    expect(content.exercises.length).toBeGreaterThan(1600);
    expect(content.warnings).toEqual([]);
  });

  it('renders all Markdown pages with balanced supported tags and Telegram length limits', () => {
    for (const lesson of content.lessons) {
      for (const markdown of Object.values(lessonSections(lesson.markdown))) {
        for (const page of markdownPages(markdown)) {
          expect(page.length, lesson.filename).toBeLessThanOrEqual(3400);
          const stack: string[] = [];
          for (const tag of page.matchAll(/<(\/?)([a-z]+)(?:\s[^>]*)?>/g)) {
            expect(['b', 'i', 's', 'code', 'pre', 'a'], lesson.filename).toContain(tag[2]);
            if (tag[1]) expect(stack.pop(), lesson.filename).toBe(tag[2]); else stack.push(tag[2]!);
          }
          expect(stack, lesson.filename).toEqual([]);
        }
      }
    }
  });

  it('renders every card face safely in both study directions', () => {
    for (const note of content.notes) {
      for (const direction of ['recognition', 'production']) {
        for (const revealed of [false, true]) {
          const text = cardText(note as unknown as Note, { direction } as UserCard, revealed);
          expect(text.length, note.term).toBeLessThan(4000);
          expect(text).not.toMatch(/<(?!\/?(?:b|code)>)/);
        }
      }
    }
  });

  it('resolves source audio and accepts alternative input forms', async () => {
    const files = [...new Set(content.notes.flatMap((n) => [...n.audio, ...n.forms.flatMap((f) => f.audio)]))];
    // Check in batches so file handles stay bounded on Windows.
    for (let i = 0; i < files.length; i += 100) {
      const statuses = await Promise.all(files.slice(i, i + 100).map((f) => stat(path.join(config.contentRoot, f))));
      expect(statuses.every((s) => s.isFile())).toBe(true);
    }
    const be = content.exercises.find((e) => e.kind === 'input' && e.prompt === 'My brother ___ (be) a doctor.');
    expect(be?.answers.map(normalizeAnswer)).toContain('is');
    expect(be?.answers).toContain("'s");
    const question = content.exercises.find((e) => e.kind === 'choice' && e.answers.includes('Have / eaten'));
    expect(question?.answers).toEqual(['Have / eaten']);
  }, 30_000);
});
