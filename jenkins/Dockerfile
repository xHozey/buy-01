FROM jenkins/jenkins:2.580.2-jdk21

USER root

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		ca-certificates \
		curl \
		docker.io \
		docker-compose \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/*

USER jenkins
