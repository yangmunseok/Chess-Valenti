FROM eclipse-temurin:25 AS assets

WORKDIR /assets

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl p7zip-full \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/data \
    && curl -fL --output ajotb.7z https://l--l.top/ajotb-pgn-000 \
    && 7z x ajotb.7z -o/app/data -y \
    && pgn_file="$(find /app/data -type f -iname '*.pgn' | head -n 1)" \
    && test -n "$pgn_file" \
    && if [ "$pgn_file" != "/app/data/AJ-OTB-PGN-001.pgn" ]; then cp "$pgn_file" /app/data/AJ-OTB-PGN-001.pgn; fi \
    && curl -fL "https://drive.google.com/uc?export=download&id=1SOAd7pQ4Gu3PYWSm_ZHMkNWP9-Y7u_40" -o csv_data.7z \
    && 7z x csv_data.7z -o/app/data -y \
    && curl -fL --output stockfish.tar https://github.com/official-stockfish/Stockfish/releases/latest/download/stockfish-ubuntu-x86-64-avx2.tar \
    && mkdir -p /tmp/stockfish \
    && tar -xf stockfish.tar -C /tmp/stockfish \
    && stockfish_bin="$(find /tmp/stockfish -type f -name 'stockfish*' | head -n 1)" \
    && test -n "$stockfish_bin" \
    && cp "$stockfish_bin" /app/data/stockfish \
    && chmod +x /app/data/stockfish


FROM maven:3.9.15-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY . .
RUN mvn clean package "-Dmaven.test.skip=true"


FROM eclipse-temurin:25

WORKDIR /app

COPY --from=build /workspace/target/chess-valenti-0.0.1-SNAPSHOT.jar app.jar
COPY --from=assets /app/data ./data

ENV CHESS_PGN_PATH=/app/data/AJ-OTB-PGN-001.pgn
ENV STOCKFISH_PATH=/app/data/stockfish
ENV CHESS_DATA_INIT_MODE=CSV_TO_DB
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
