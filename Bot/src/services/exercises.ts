import type { PrismaClient } from "../generated/prisma/client.js";
import { strings, token, UserError } from "../utils.js";

export function normalizeAnswer(value: string): string {
  return value
    .normalize("NFKC")
    .toLowerCase()
    .trim()
    .replace(/[‘’`]/g, "'")
    .replace(/\b(can't)\b/g, "cannot")
    .replace(/\bwon't\b/g, "will not")
    .replace(/\bshan't\b/g, "shall not")
    .replace(/n't\b/g, " not")
    .replace(/\bi'm\b/g, "i am")
    .replace(/\b(you|we|they)'re\b/g, "$1 are")
    .replace(/\b(i|you|we|they)'ve\b/g, "$1 have")
    .replace(/\b(i|you|he|she|it|we|they)'ll\b/g, "$1 will")
    .replace(/[—–]/g, "-")
    .replace(/\s*\/\s*/g, " / ")
    .replace(/[.!?]+$/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

export class ExerciseService {
  constructor(private db: PrismaClient) {}

  async next(userId: number, lessonId?: number) {
    // Prefer exercises never attempted, then mistakes, then the least recently attempted.
    const exercises = await this.db.exercise.findMany({
      where: { active: true, ...(lessonId ? { lessonId } : {}) },
      include: {
        attempts: {
          where: { userId },
          orderBy: { createdAt: "desc" },
          take: 1,
        },
      },
      orderBy: { id: "asc" },
    });
    exercises.sort((a, b) => {
      const aa = a.attempts[0];
      const bb = b.attempts[0];
      const rank = (attempt: typeof aa) =>
        !attempt ? 0 : attempt.correct ? 2 : 1;
      return (
        rank(aa) - rank(bb) ||
        (aa?.createdAt.getTime() ?? 0) - (bb?.createdAt.getTime() ?? 0)
      );
    });
    const exercise = exercises[0];
    if (!exercise) return null;
    return this.db.exerciseSession.upsert({
      where: { userId },
      create: {
        userId,
        token: token(),
        exerciseId: exercise.id,
        lessonId: lessonId ?? null,
      },
      update: {
        token: token(),
        exerciseId: exercise.id,
        lessonId: lessonId ?? null,
        createdAt: new Date(),
      },
      include: { exercise: true },
    });
  }

  async answer(userId: number, sessionToken: string, answer: string) {
    return this.db.$transaction(async (tx) => {
      const session = await tx.exerciseSession.findUnique({
        where: { userId },
        include: { exercise: true },
      });
      if (
        !session ||
        session.token !== sessionToken ||
        !session.exercise.active
      )
        throw new UserError("Это задание уже закрыто. Нажмите /practice.");
      const expected = strings(session.exercise.answers);
      const correct = expected.some(
        (value) => normalizeAnswer(value) === normalizeAnswer(answer),
      );
      await tx.exerciseAttempt.create({
        data: { userId, exerciseId: session.exerciseId, answer, correct },
      });
      await tx.exerciseSession.delete({ where: { userId } });
      return { ...session, correct, expected };
    });
  }
}
