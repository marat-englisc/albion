import { describe, expect, it } from 'vitest';
import { AudioIndex } from '../src/content/audio-index.js';
import { parseExercises } from '../src/content/load.js';
import { cleanObsidian, lessonSections, markdownPages } from '../src/content/markdown.js';
import { normalizeAnswer } from '../src/services/exercises.js';

describe('content handling', () => {
  it('keeps multi-gap answers together', () => {
    const rows = [['№', 'Уровень', 'Номер темы', 'Тема', 'Тип', 'Задание', 'Вариант 1', 'Вариант 2', 'Правильный ответ', 'Пояснение'],
      ['1', 'A1', '1', 'Questions', 'выбор', '___ you ___?', 'Have / eaten', 'Did / ate', 'Have / eaten', 'Perfect']];
    expect(parseExercises(rows)[0]?.answers).toEqual(['Have / eaten']);
  });
  it('accepts single-gap variants and newline alternatives for several gaps', () => {
    const head = ['№', 'Уровень', 'Номер темы', 'Тема', 'Тип', 'Задание', 'Правильный ответ', 'Пояснение'];
    const rows = [head, ['1', 'A1', '1', 'Be', 'ввод', 'He ___ a doctor.', "is / 's", ''],
      ['2', 'A1', '1', 'Articles', 'ввод', '___ Alps, ___ Everest', 'The / —\nThe / -', '']];
    expect(parseExercises(rows)[0]?.answers).toEqual(['is', "'s"]);
    expect(parseExercises(rows)[1]?.answers).toEqual(['The / —', 'The / -']);
  });
  it('matches alternative verb forms and uses past pronunciation', () => {
    const audio = new AudioIndex(['read.mp3', 'read (past).mp3', 'was.mp3', 'were.mp3'], 'irregular_verbs');
    expect(audio.verb('was / were', true)).toEqual(['audio/irregular_verbs/was.mp3', 'audio/irregular_verbs/were.mp3']);
    expect(audio.find('read', '', true)).toEqual(['audio/irregular_verbs/read (past).mp3']);
  });
  it('hides answer callouts and translates Obsidian markup', () => {
    const source = '# Lesson\n\nQuestion\n\n> [!success]- Ответы и пояснения\n> **Secret**\n\n## More\nText';
    const sections = lessonSections(source);
    expect(sections.main).not.toContain('Secret'); expect(sections.answers).toContain('Secret');
    expect(cleanObsidian('[[001 - Topic|Тема]] ==word==')).toBe('Тема **word**');
  });
  it('escapes HTML and supports Telegram-compatible formatting', () => {
    const html = markdownPages('# Title\n\n**bold** and *italic* and `code`\n\n<script>x</script>\n\n[bad](javascript:alert) [good](https://example.com)')[0]!;
    expect(html).toContain('<b>bold</b>'); expect(html).toContain('<i>italic</i>');
    expect(html).toContain('<code>code</code>'); expect(html).not.toContain('<script>'); expect(html).not.toContain('href="javascript:');
  });
  it('paginates without breaking tags, entities or Unicode characters', () => {
    const pages = markdownPages(`**${'a & < 🦊 '.repeat(900)}**`, 500);
    expect(pages.length).toBeGreaterThan(1);
    for (const page of pages) { expect(page.length).toBeLessThanOrEqual(500); expect(page).not.toMatch(/&(?!amp;|lt;|gt;|quot;)/); }
    expect(markdownPages('a'.repeat(500), 500)).toEqual(['a'.repeat(500)]);
  });
  it('normalizes case, punctuation, apostrophes and unambiguous contractions', () => {
    expect(normalizeAnswer('  I DON’T know. ')).toBe(normalizeAnswer('I do not know'));
    expect(normalizeAnswer('Have/eaten')).toBe(normalizeAnswer('Have / eaten'));
    expect(normalizeAnswer('I can’t.')).toBe(normalizeAnswer('I cannot'));
  });
});
