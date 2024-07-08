FROM eclipse-temurin:11.0.22_7-jre-alpine
ARG TF_VERSION=1.5.7

ARG USER_ID=65535
ARG GROUP_ID=65535
ARG USER_NAME=javauser
ARG GROUP_NAME=javauser

RUN addgroup -g $GROUP_ID $GROUP_NAME && \
    adduser --shell /sbin/nologin --disabled-password \
    --uid $USER_ID --ingroup $GROUP_NAME $USER_NAME

RUN apk add --no-cache --update python3 py3-pip bash
# azure cli
RUN apk add --no-cache -q --virtual=build gcc musl-dev python3-dev libffi-dev openssl-dev cargo make \
     && pip install --no-cache-dir --prefer-binary --break-system-packages -q azure-cli \
     && apk del --purge build

WORKDIR /tmp
RUN wget https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_amd64.zip && \
    unzip terraform_${TF_VERSION}_linux_amd64.zip -d /usr/local/bin/ && \
    chmod +x /usr/local/bin/terraform && \
    rm terraform_${TF_VERSION}_linux_amd64.zip

WORKDIR /service

COPY --chown=${USER_NAME}:${GROUP_NAME} terraform/target/universal/stage /service

USER ${USER_NAME}

ENTRYPOINT ["/service/bin/terraform-provisioner", "-Dlogback.configurationFile=/config/logback.xml", "-Dconfig.file=/config/application.conf"]
