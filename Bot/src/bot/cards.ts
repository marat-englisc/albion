import { InlineKeyboard } from 'grammy';
import type { Note, UserCard } from '../generated/prisma/client.js';
import type { VerbForm } from '../content/types.js';
import { escapeHtml as h, strings, interval } from '../utils.js';
import { schedule, ratings, ratingLabels } from '../services/scheduler.js';

export function cardText(note: Note, card: Pick<UserCard, 'direction'>, revealed: boolean): string {
  const irregular = note.deckId === 'irregular_verbs';
  const heading = `${h(note.level || 'Все уровни')}${note.partOfSpeech ? ` · ${h(note.partOfSpeech)}` : ''}`;
  const prompt = card.direction === 'production' ? note.translation : note.term;
  let text = `${heading}\n\n<b>${h(prompt)}</b>`;
  if (!revealed) return text + (irregular ? '\n\nВспомните перевод и три формы глагола.' : card.direction === 'production' ? '\n\nВспомните английское слово или выражение.' : '\n\nВспомните перевод.');
  text += `\n\n<b>${h(card.direction === 'production' ? note.term : note.translation)}</b>`;
  if (note.ipa) text += `\n${h(note.ipa)}`;
  if (note.definition) text += `\n\n${h(note.definition)}`;
  const forms = Array.isArray(note.forms) ? note.forms as unknown as VerbForm[] : [];
  if (forms.length) text += '\n\n' + forms.map((f) => `<b>${h(f.label)}</b>: ${h(f.text)} ${h(f.ipa)}`).join('\n');
  const examples = strings(note.examples).slice(0, irregular ? 3 : 2);
  if (examples.length) text += '\n\n' + examples.map((s) => `• ${h(s)}`).join('\n');
  if (note.comment) text += `\n\n${h(note.comment)}`;
  if (text.length > 3900) {
    // The source has rich examples; keep the core answer and safely shorten plain text.
    return `${heading}\n\n<b>${h(note.term)}</b>\n${h(note.translation)}\n${h(note.ipa)}\n\n${h(note.definition.slice(0, 900))}\n\n${forms.map((f) => `${h(f.label)}: ${h(f.text)}`).join('\n')}`;
  }
  return text;
}

export function cardKeyboard(card: UserCard, retention: number, sessionToken: string, revealed: boolean, now = new Date()) {
  const keyboard = new InlineKeyboard();
  if (!revealed) keyboard.text('Показать ответ', `show:${sessionToken}`).row();
  else {
    for (const [i, rating] of ratings.entries()) {
      keyboard.text(`${ratingLabels[i]} · ${interval(schedule(card, retention, rating, now).card.due, now)}`, `rate:${sessionToken}:${rating}`);
      if (i === 1 || i === 3) keyboard.row();
    }
  }
  if (revealed || card.direction === 'recognition') keyboard.text('🔊 Озвучить', `audio:${sessionToken}`).row();
  return keyboard.text('Отложить карточку', `suspend:${sessionToken}`).row().text('Завершить', `stop:${sessionToken}`);
}
