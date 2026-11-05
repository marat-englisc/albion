import { config } from "../config.js";
import { loadContent } from "../content/load.js";
import { DECKS } from "../content/types.js";

const content = await loadContent(config.contentRoot);

console.table(
  DECKS.map((d) => ({
    Набор: d.title,
    Карточек: content.notes.filter((n) => n.deckId === d.id).length,
  })),
);

console.log(
  `Уроков: ${content.lessons.length}. Упражнений: ${content.exercises.length}.`,
);
for (const warning of content.warnings) console.warn(warning);
