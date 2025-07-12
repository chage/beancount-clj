FROM clojure:openjdk-17-tools-deps

ARG USER_ID=1000
ARG GROUP_ID=1000
ARG USER_NAME=hostuser
ARG GROUP_NAME=hostgroup

ENV APP_HOME_DIR=/app

RUN apt-get update && apt-get -y install python3 python3-venv python3-pip \
    && pip3 install beancount \
    && rm -rf /var/lib/apt/lists/*

WORKDIR ${APP_HOME_DIR}

# create app user and app home
RUN groupadd --gid ${GROUP_ID} ${GROUP_NAME} \
    && useradd --uid ${USER_ID} --gid ${GROUP_ID} --home-dir ${APP_HOME_DIR} --shell /bin/bash ${USER_NAME} \
    && chown -R ${USER_NAME}:${GROUP_NAME} ${APP_HOME_DIR}

USER ${USER_NAME}

CMD ["/bin/bash"]
