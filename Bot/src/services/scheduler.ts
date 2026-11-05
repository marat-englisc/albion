import { fsrs, type Card, type Grade, type RecordLogItem } from "ts-fsrs";
import type { UserCard } from "../generated/prisma/client.js";

export function toFsrs(card: UserCard): Card {
  return {
    due: card.due,
    stability: card.stability,
    difficulty: card.difficulty,
    elapsed_days: card.elapsedDays,
    scheduled_days: card.scheduledDays,
    learning_steps: card.learningSteps,
    reps: card.reps,
    lapses: card.lapses,
    state: card.state,
    last_review: card.lastReview ?? undefined,
  };
}

export function fromFsrs(card: Card) {
  return {
    due: card.due,
    stability: card.stability,
    difficulty: card.difficulty,
    elapsedDays: card.elapsed_days,
    scheduledDays: card.scheduled_days,
    learningSteps: card.learning_steps,
    reps: card.reps,
    lapses: card.lapses,
    state: card.state,
    lastReview: card.last_review ?? null,
  };
}

export function schedule(
  card: UserCard,
  retention: number,
  rating: Grade,
  now: Date,
): RecordLogItem {
  return fsrs({
    request_retention: retention,
    enable_fuzz: false,
    enable_short_term: true,
    learning_steps: ["1m", "10m"],
    relearning_steps: ["10m"],
  }).next(toFsrs(card), now, rating);
}

export const ratings = [1, 2, 3, 4] as const;
export const ratingLabels = ["Снова", "Трудно", "Хорошо", "Легко"] as const;
