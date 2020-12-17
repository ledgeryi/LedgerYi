FROM anapsix/alpine-java:8u202b08_jdk

MAINTAINER LedgerYi <ledgeryi@chainboard.cn>

RUN mkdir -p /ledgeryi/data \
    && mkdir /ledgeryi/app \
    && mkdir /ledgeryi/data/output-directory

WORKDIR /ledgeryi/data

COPY node-latest.jar /ledgeryi/app/node.jar

ENTRYPOINT [ "java", "-Xms128m" ,"-Xmx2048m", "-jar", "/ledgeryi/app/node.jar" ]

EXPOSE 50051
EXPOSE 6666
EXPOSE 8080