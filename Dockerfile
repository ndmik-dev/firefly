FROM eclipse-temurin:25-jdk-jammy

WORKDIR /app

# Playwright runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    libnss3 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    libpangocairo-1.0-0 \
    libpango-1.0-0 \
    libcairo2 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxext6 \
    libxss1 \
    libxtst6 \
    libdrm2 \
    libgtk-3-0 \
    libxkbcommon0 \
    libglib2.0-0 \
    fonts-liberation \
  && rm -rf /var/lib/apt/lists/*

COPY . /app
RUN ./gradlew --no-daemon bootJar -x test

ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/db/app.db
RUN mkdir -p /app/db \
  && if [ -f /app/src/main/resources/db/app.db ]; then cp /app/src/main/resources/db/app.db /app/db/app.db; fi
VOLUME ["/app/db"]

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/build/libs/dtek-telegram-bot-0.0.1.jar"]
