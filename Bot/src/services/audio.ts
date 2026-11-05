import path from "node:path";
import { realpath, stat } from "node:fs/promises";
import { InputFile, GrammyError } from "grammy";
import type { PrismaClient, Note } from "../generated/prisma/client.js";
import type { BotContext } from "../bot/context.js";
import type { VerbForm } from "../content/types.js";
import { strings, UserError } from "../utils.js";
import { config } from "../config.js";

export class AudioService {
  constructor(private db: PrismaClient) {}
  async send(ctx: BotContext, note: Note, allForms: boolean) {
    const forms = Array.isArray(note.forms)
      ? (note.forms as unknown as VerbForm[])
      : [];
    const files = [
      ...new Set([
        ...strings(note.audio),
        ...(allForms ? forms.flatMap((f) => f.audio) : []),
      ]),
    ];
    if (!files.length) throw new UserError("Для этой карточки нет записи.");
    for (const relative of files) {
      const root = await realpath(path.join(config.contentRoot, "audio"));
      const filename = await realpath(
        path.resolve(config.contentRoot, relative),
      );
      const inside = path.relative(root, filename);
      if (inside.startsWith("..") || path.isAbsolute(inside))
        throw new Error("Аудиофайл находится за пределами audio/");
      const info = await stat(filename);
      const signature = `${info.size}:${info.mtimeMs}`;
      const cached = await this.db.audioCache.findUnique({
        where: { path: relative },
      });
      let file: string | InputFile =
        cached?.signature === signature
          ? cached.fileId
          : new InputFile(filename);
      let result;
      try {
        result = await ctx.replyWithAudio(file, {
          title: path.parse(filename).name,
          performer: "Albion",
          protect_content: false,
        });
      } catch (error) {
        if (
          !(error instanceof GrammyError) ||
          error.error_code !== 400 ||
          typeof file !== "string"
        )
          throw error;
        file = new InputFile(filename);
        result = await ctx.replyWithAudio(file, {
          title: path.parse(filename).name,
          performer: "Albion",
        });
      }
      if (result.audio)
        await this.db.audioCache.upsert({
          where: { path: relative },
          create: { path: relative, signature, fileId: result.audio.file_id },
          update: { signature, fileId: result.audio.file_id },
        });
    }
  }
}
