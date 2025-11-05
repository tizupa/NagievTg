# Этап сборки (build stage) — используем Maven для компиляции
FROM maven:3.9.9-amazoncorretto-21 AS build

# Копируем pom.xml и исходный код
COPY pom.xml /app/
COPY src /app/src/

# Собираем JAR (игнорируем тесты, если не нужны)
WORKDIR /app
RUN mvn clean package -DskipTests

# Этап запуска (runtime stage) — лёгкий образ без Maven
FROM amazoncorretto:21-alpine-jdk

# Копируем собранный JAR из build-stage
COPY --from=build /app/target/*.jar /app/app.jar

# Устанавливаем рабочую директорию и точку входа
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]

