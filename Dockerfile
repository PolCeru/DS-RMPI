FROM openjdk:22-slim

LABEL authors="Bramb"

COPY out/artifacts/Polimi_DS_RMPI_test_jar /opt

WORKDIR /opt

CMD ["java", "-jar", "Polimi-DS-RMPI.jar"]