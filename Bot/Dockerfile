FROM node:24-bookworm-slim
WORKDIR /app/Bot
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . ./
RUN npm run db:generate && npm run build
ENV NODE_ENV=production
ENV CONTENT_ROOT=/app/Bot/content
ENV DATABASE_URL=file:./data/albion.db
CMD ["sh", "-c", "npm run db:migrate && npm run content:import && exec node dist/index.js"]
