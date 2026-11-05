import path from "node:path";

export class AudioIndex {
  constructor(
    private files: string[],
    private directory: string,
  ) {}

  find(term: string, partOfSpeech = "", past = false): string[] {
    const base = (s: string) =>
      path.parse(s).name.split(" (")[0]!.toLowerCase();
    const candidates = this.files.filter(
      (f) => base(f) === term.trim().toLowerCase(),
    );
    const qualified = candidates.filter((f) => /\s\(/.test(f));
    const pastFiles = qualified.filter((f) => /\(past\)/i.test(f));
    let selected: string[];
    if (past && pastFiles.length) selected = pastFiles;
    else {
      const precise = qualified.find(
        (f) =>
          partOfSpeech &&
          f.toLowerCase().includes(`(${partOfSpeech.toLowerCase()})`),
      );
      const sameCase = candidates.find(
        (f) => path.parse(f).name === term.trim(),
      );
      const sameCaseQualified = qualified.find((f) =>
        path.parse(f).name.startsWith(`${term.trim()} (`),
      );
      const bare = candidates.find((f) => !/\s\(/.test(f));
      const first =
        precise ?? sameCase ?? sameCaseQualified ?? bare ?? candidates[0];
      selected = first ? [first] : [];
    }
    return selected.map((f) => path.posix.join("audio", this.directory, f));
  }

  verb(text: string, past: boolean): string[] {
    return [
      ...new Set(
        text
          .split(/\s*\/\s*/)
          .flatMap((variant) => [
            ...this.find(variant, "", past),
            ...this.files
              .filter(
                (f) =>
                  path
                    .parse(f)
                    .name.toLowerCase()
                    .startsWith(`${variant.toLowerCase()} (`) &&
                  !/\(past\)/i.test(f),
              )
              .map((f) => path.posix.join("audio", this.directory, f)),
          ]),
      ),
    ];
  }
}
