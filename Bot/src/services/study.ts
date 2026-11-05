import type {
  Prisma,
  PrismaClient,
  User,
  UserCard,
} from "../generated/prisma/client.js";
import { localDay, token, UserError } from "../utils.js";
import { fromFsrs, schedule } from "./scheduler.js";
import type { Grade } from "ts-fsrs";

export function noteFilter(user: User): Prisma.NoteWhereInput {
  return {
    active: true,
    ...(user.activeDeck !== "all" ? { deckId: user.activeDeck } : {}),
    ...(user.level !== "all"
      ? { OR: [{ level: user.level }, { level: "" }] }
      : {}),
  };
}

export class StudyService {
  constructor(private db: PrismaClient) {}

  async current(userId: number) {
    return this.db.studySession.findUnique({
      where: { userId },
      include: { card: { include: { note: { include: { deck: true } } } } },
    });
  }

  async next(user: User, now = new Date()) {
    const existing = await this.current(user.id);
    if (
      existing &&
      !existing.card.suspended &&
      existing.card.direction === user.direction &&
      existing.card.note.active
    )
      return existing;
    const filter = noteFilter(user);
    let card = await this.db.userCard.findFirst({
      where: {
        userId: user.id,
        direction: user.direction,
        suspended: false,
        state: { not: 0 },
        due: { lte: now },
        note: filter,
      },
      orderBy: [{ due: "asc" }, { id: "asc" }],
    });
    if (!card) {
      const introduced = await this.db.userCard.count({
        where: { userId: user.id, introducedDay: localDay(now, user.timezone) },
      });
      if (introduced < user.newCardsPerDay) {
        const note = await this.db.note.findFirst({
          where: {
            ...filter,
            cards: {
              none: {
                userId: user.id,
                direction: user.direction,
                OR: [{ state: { not: 0 } }, { suspended: true }],
              },
            },
          },
          orderBy: [{ deck: { position: "asc" } }, { sourceRow: "asc" }],
        });
        if (note)
          card = await this.db.userCard.upsert({
            where: {
              userId_noteId_direction: {
                userId: user.id,
                noteId: note.id,
                direction: user.direction,
              },
            },
            create: {
              userId: user.id,
              noteId: note.id,
              direction: user.direction,
              due: now,
            },
            update: {},
          });
      }
    }

    if (!card) {
      await this.db.studySession.deleteMany({ where: { userId: user.id } });
      return null;
    }

    await this.db.studySession.upsert({
      where: { userId: user.id },
      create: {
        userId: user.id,
        cardId: card.id,
        token: token(),
        createdAt: now,
      },
      update: {
        cardId: card.id,
        token: token(),
        revealed: false,
        createdAt: now,
      },
    });
    return this.current(user.id);
  }

  async reveal(userId: number, sessionToken: string) {
    const changed = await this.db.studySession.updateMany({
      where: { userId, token: sessionToken },
      data: { revealed: true },
    });
    if (!changed.count)
      throw new UserError("Эта карточка уже закрыта. Нажмите /study.");
    return this.current(userId);
  }

  async rate(
    user: User,
    sessionToken: string,
    rating: Grade,
    now = new Date(),
  ) {
    if (![1, 2, 3, 4].includes(rating))
      throw new UserError("Неизвестная оценка.");

    return this.db.$transaction(async (tx) => {
      const session = await tx.studySession.findUnique({
        where: { userId: user.id },
        include: { card: { include: { note: true } } },
      });
      if (
        !session ||
        session.token !== sessionToken ||
        !session.revealed ||
        session.card.suspended ||
        !session.card.note.active
      ) {
        throw new UserError(
          "Сначала откройте ответ на текущей карточке: /study.",
        );
      }
      const { card } = session;
      const result = schedule(card, user.retention, rating, now);
      const changed = await tx.userCard.updateMany({
        where: { id: card.id, version: card.version },
        data: {
          ...fromFsrs(result.card),
          introducedDay: card.introducedDay ?? localDay(now, user.timezone),
          version: { increment: 1 },
        },
      });
      if (!changed.count)
        throw new UserError("Оценка уже сохранена. Нажмите /study.");
      const previous = snapshot(card);
      await tx.reviewLog.create({
        data: {
          userId: user.id,
          cardId: card.id,
          rating,
          reviewedAt: now,
          localDay: localDay(now, user.timezone),
          previous: JSON.parse(
            JSON.stringify(previous),
          ) as Prisma.InputJsonObject,
          fsrsLog: JSON.parse(
            JSON.stringify(result.log),
          ) as Prisma.InputJsonObject,
          versionAfter: card.version + 1,
        },
      });
      await tx.studySession.delete({ where: { userId: user.id } });
      return result.card;
    });
  }

  async suspend(userId: number, sessionToken: string) {
    await this.db.$transaction(async (tx) => {
      const session = await tx.studySession.findUnique({ where: { userId } });
      if (!session || session.token !== sessionToken)
        throw new UserError("Карточка уже закрыта.");
      await tx.userCard.update({
        where: { id: session.cardId },
        data: { suspended: true, version: { increment: 1 } },
      });
      await tx.studySession.delete({ where: { userId } });
    });
  }

  async undo(userId: number) {
    await this.db.$transaction(async (tx) => {
      const log = await tx.reviewLog.findFirst({
        where: { userId, undone: false },
        orderBy: { id: "desc" },
        include: { card: true },
      });
      if (!log) throw new UserError("Нет оценки для отмены.");
      if (log.card.version !== log.versionAfter)
        throw new UserError(
          "Эту оценку уже нельзя отменить: карточка изменилась.",
        );
      const previous = log.previous as unknown as ReturnType<typeof snapshot>;
      await tx.userCard.update({
        where: { id: log.cardId },
        data: {
          ...previous,
          due: new Date(previous.due),
          lastReview: previous.lastReview
            ? new Date(previous.lastReview)
            : null,
          version: { increment: 1 },
        },
      });
      await tx.reviewLog.update({
        where: { id: log.id },
        data: { undone: true },
      });
      await tx.studySession.upsert({
        where: { userId },
        create: { userId, cardId: log.cardId, token: token() },
        update: {
          cardId: log.cardId,
          token: token(),
          revealed: false,
          createdAt: new Date(),
        },
      });
    });
  }

  async summary(user: User, now = new Date()) {
    const filter = {
      userId: user.id,
      direction: user.direction,
      suspended: false,
      note: noteFilter(user),
    };
    const [due, upcoming, introduced] = await Promise.all([
      this.db.userCard.count({
        where: { ...filter, state: { not: 0 }, due: { lte: now } },
      }),
      this.db.userCard.findFirst({
        where: { ...filter, state: { not: 0 }, due: { gt: now } },
        orderBy: { due: "asc" },
      }),
      this.db.userCard.count({
        where: { userId: user.id, introducedDay: localDay(now, user.timezone) },
      }),
    ]);
    return {
      due,
      upcoming: upcoming?.due,
      newRemaining: Math.max(0, user.newCardsPerDay - introduced),
    };
  }
}

function snapshot(card: UserCard) {
  return {
    due: card.due.toISOString(),
    stability: card.stability,
    difficulty: card.difficulty,
    elapsedDays: card.elapsedDays,
    scheduledDays: card.scheduledDays,
    learningSteps: card.learningSteps,
    reps: card.reps,
    lapses: card.lapses,
    state: card.state,
    lastReview: card.lastReview?.toISOString() ?? null,
    introducedDay: card.introducedDay,
  };
}
