import type { PrismaClient } from "../generated/prisma/client.js";
import { DECKS, type Content } from "./types.js";

export async function importContent(
  db: PrismaClient,
  content: Content,
): Promise<void> {
  // Parse and validate all files before starting this transaction. Archiving keeps review history intact.
  await db.$transaction(
    async (tx) => {
      for (const [position, deck] of DECKS.entries()) {
        await tx.deck.upsert({
          where: { id: deck.id },
          update: { title: deck.title, position },
          create: { id: deck.id, title: deck.title, position },
        });
      }
      await tx.note.updateMany({ data: { active: false } });
      await tx.lesson.updateMany({ data: { active: false } });
      await tx.exercise.updateMany({ data: { active: false } });
      for (const note of content.notes) {
        const data = {
          ...note,
          forms: note.forms.map((f) => ({ ...f })),
          active: true,
        };
        await tx.note.upsert({
          where: { sourceKey: note.sourceKey },
          create: data,
          update: data,
        });
      }
      for (const lesson of content.lessons) {
        const data = { ...lesson, active: true };
        await tx.lesson.upsert({
          where: { id: lesson.id },
          create: data,
          update: data,
        });
      }
      for (const exercise of content.exercises) {
        const data = { ...exercise, active: true };
        await tx.exercise.upsert({
          where: { sourceKey: exercise.sourceKey },
          create: data,
          update: data,
        });
      }
    },
    { timeout: 180_000 },
  );
}
