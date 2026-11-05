import type { Context } from 'grammy';
import type { User } from '../generated/prisma/client.js';
export type BotContext = Context & { user: User };
