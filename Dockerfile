FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

COPY gradlew settings.gradle build.gradle /app/
COPY gradle /app/gradle
COPY src /app/src

RUN mkdir -p /ms-playwright
RUN ./gradlew --no-daemon playwrightInstallChromium
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

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

COPY --from=builder /ms-playwright /ms-playwright
COPY --from=builder /app/build/libs/firefly-0.0.1.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
