import type { PrismaClient, User } from "../generated/prisma/client.js";
import { localDay } from "../utils.js";

export async function statistics(
  db: PrismaClient,
  user: User,
  now = new Date(),
) {
  const [reviews, cards, completed, attempts] = await Promise.all([
    db.reviewLog.findMany({
      where: { userId: user.id, undone: false },
      select: { rating: true, reviewedAt: true },
    }),
    db.userCard.groupBy({
      by: ["state"],
      where: { userId: user.id, suspended: false, note: { active: true } },
      _count: true,
    }),
    db.lessonProgress.count({
      where: {
        userId: user.id,
        completedAt: { not: null },
        lesson: { active: true },
      },
    }),
    db.exerciseAttempt.findMany({
      where: { userId: user.id },
      select: { correct: true },
    }),
  ]);

  const days = new Set(
    reviews.map((r) => localDay(r.reviewedAt, user.timezone)),
  );
  const today = localDay(now, user.timezone);
  const todays = reviews.filter(
    (r) => localDay(r.reviewedAt, user.timezone) === today,
  );

  // Work in calendar dates so DST does not turn a day into 23 or 25 hours.
  let cursor = Date.parse(`${today}T12:00:00Z`);

  if (!days.has(today)) cursor -= 86_400_000;

  let streak = 0;

  while (days.has(new Date(cursor).toISOString().slice(0, 10))) {
    streak++;
    cursor -= 86_400_000;
  }
  return {
    total: reviews.length,
    today: todays.length,
    streak,
    completed,
    recall: todays.length
      ? Math.round(
          (todays.filter((r) => r.rating > 1).length / todays.length) * 100,
        )
      : null,
    learning: cards
      .filter((c) => c.state === 1 || c.state === 3)
      .reduce((n, c) => n + c._count, 0),
    learned: cards.find((c) => c.state === 2)?._count ?? 0,
    attempts: attempts.length,
    correct: attempts.filter((a) => a.correct).length,
  };
}
