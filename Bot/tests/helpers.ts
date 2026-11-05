import { mkdtemp, readFile, rm } from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import { DatabaseSync } from 'node:sqlite';
import { createDatabase } from '../src/db.js';
import { botRoot } from '../src/config.js';
import type { Content, ImportedNote } from '../src/content/types.js';

export async function testDatabase() {
  const directory = await mkdtemp(path.join(os.tmpdir(), 'albion-bot-test-'));
  const filename = path.join(directory, 'test.db');
  const raw = new DatabaseSync(filename);
  raw.exec(await readFile(path.join(botRoot, 'prisma/migrations/20261105120000_init/migration.sql'), 'utf8'));
  raw.close();
  const db = createDatabase(`file:${filename}`);
  return { db, close: async () => {
    await db.$disconnect();
    if (path.dirname(path.resolve(directory)) !== path.resolve(os.tmpdir()) || !path.basename(directory).startsWith('albion-bot-test-')) throw new Error('Unexpected test directory');
    await rm(directory, { recursive: true, force: true });
  } };
}

export const sampleNote = (overrides: Partial<ImportedNote> = {}): ImportedNote => ({
  sourceKey: 'apple', deckId: 'words', term: 'apple', ipa: '/ˈæpəl/', partOfSpeech: 'noun',
  definition: 'a fruit', translation: 'яблоко', level: 'A1', examples: ['I eat an apple.'],
  forms: [], audio: [], comment: '', sourceRow: 2, ...overrides,
});
export const sampleContent = (): Content => ({ notes: [sampleNote(), sampleNote({ sourceKey: 'book', term: 'book', translation: 'книга', sourceRow: 3 })],
  lessons: [{ id: 1, title: 'Word order', level: 'A1', filename: '001.md', markdown: '# Word order\n\n**Subject** + verb.\n\n> [!success]- Ответы\n> Answer.\n', keywords: 'order' }],
  exercises: [{ sourceKey: 'grammar:1', lessonId: 1, topic: 'Word order', level: 'A1', kind: 'choice', prompt: 'He ___ a book.', options: ['reads', 'read'], answers: ['reads'], explanation: 'He takes -s.' },
    { sourceKey: 'grammar:2', lessonId: 1, topic: 'Questions', level: 'A1', kind: 'input', prompt: '___ you ___?', options: [], answers: ['Have / eaten'], explanation: 'Present Perfect.' }], warnings: [] });
