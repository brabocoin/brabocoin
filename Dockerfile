FROM java:8
WORKDIR /
ADD core/build/libs/brabocoin-*-headless.jar brabocoin-dictator.jar
EXPOSE 56129
CMD java -jar brabocoin-dictator.jar
