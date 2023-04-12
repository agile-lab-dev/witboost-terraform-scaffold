FROM eclipse-temurin:11.0.18_10-jre-alpine
ARG TF_VERSION=1.4.4

ARG USER_ID=65535
ARG GROUP_ID=65535
ARG USER_NAME=javauser
ARG GROUP_NAME=javauser

RUN addgroup -g $GROUP_ID $GROUP_NAME && \
    adduser --shell /sbin/nologin --disabled-password \
    --no-create-home --uid $USER_ID --ingroup $GROUP_NAME $USER_NAME

RUN apk add --no-cache bash

COPY --chown=${USER_NAME}:${GROUP_NAME} terraform/target/universal/stage /service

WORKDIR /tmp
RUN wget https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_amd64.zip && \
    unzip terraform_${TF_VERSION}_linux_amd64.zip -d /usr/local/bin/ && \
    chmod +x /usr/local/bin/terraform && \
    rm terraform_${TF_VERSION}_linux_amd64.zip

WORKDIR /service

USER ${USER_NAME}

ENTRYPOINT ["/service/bin/terraform-provisioner", "-Dconfig.file=/config/application.conf"]
