# Construcción (Build)
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copia los archivos de Maven Wrapper y dependencias
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x mvnw

# Descarga las dependencias
RUN ./mvnw dependency:go-offline -B

# Copia el código fuente de la aplicación
COPY src src

# Compila el paquete empaquetando el JAR ejecutable (omite tests)
RUN ./mvnw package -DskipTests

# Ejecución Ligera (Runtime)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Zona horaria de México (America/Mexico_City)
RUN apk add --no-cache tzdata && cp /usr/share/zoneinfo/America/Mexico_City /etc/localtime
ENV TZ=America/Mexico_City

# Usuario sin privilegios para ejecutar la aplicación
RUN addgroup -S kronocash-personal && adduser -S -G kronocash-personal kronocash-personal

# Copia el JAR generado desde la etapa de compilación
COPY --from=build --chown=kronocash-personal:kronocash-personal /app/target/*.jar app.jar

# Puerto de la app Spring Boot
EXPOSE 7777

# Ejecución de la aplicación con usuario no privilegiado
USER kronocash-personal
ENTRYPOINT ["java", "-jar", "app.jar"]