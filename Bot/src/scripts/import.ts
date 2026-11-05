import { config } from '../config.js';
import { createDatabase, configureDatabase } from '../db.js';
import { loadContent } from '../content/load.js';
import { importContent } from '../content/import.js';

const db = createDatabase();
try {
  const content = await loadContent(config.contentRoot);
  await configureDatabase(db);
  await importContent(db, content);
  console.log(`Импортировано: ${content.notes.length} карточек, ${content.lessons.length} уроков, ${content.exercises.length} упражнений.`);
  for (const warning of content.warnings) console.warn(warning);
} finally { await db.$disconnect(); }
