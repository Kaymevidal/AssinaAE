FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src/main src/main
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# LibreOffice converte DOCX -> PDF; pdf2docx (Python) converte PDF -> DOCX,
# já que o LibreOffice não tem filtro de exportação de PDF pra Writer.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libreoffice-writer fonts-liberation python3 python3-pip \
    && pip3 install --no-cache-dir pdf2docx==0.5.13 \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
