import { InlineKeyboard, Keyboard } from 'grammy';
import { DECKS } from '../content/types.js';

export const labels = { study: '📚 Учить', decks: '🗂 Наборы', grammar: '📖 Грамматика', practice: '✍️ Практика', stats: '📊 Прогресс', settings: '⚙️ Настройки' };
export const mainKeyboard = new Keyboard().text(labels.study).text(labels.decks).row()
  .text(labels.grammar).text(labels.practice).row().text(labels.stats).text(labels.settings).resized().persistent();
export function deckKeyboard(active: string) {
  const keyboard = new InlineKeyboard().text(`${active === 'all' ? '✓ ' : ''}Все наборы`, 'deck:all').row();
  for (const deck of DECKS) keyboard.text(`${active === deck.id ? '✓ ' : ''}${deck.title}`, `deck:${deck.id}`).row();
  return keyboard;
}
