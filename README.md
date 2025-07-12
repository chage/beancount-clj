# beancount-clj

This project initially aimed to port [Beancount](https://github.com/beancount/beancount) into a Clojure library, but due to the extensive functionality, it shifted to using [libpython-clj](https://github.com/clj-python/libpython-clj) to load Beancount instead.

## Getting Started

### Add beancount-clj as a dependency

```
{:deps {clj-python/libpython-clj {:mvn/version "2.024"}
        com.github.chage/beancount-clj {:git/sha "..."}
```

### Python

Currently you need to run under Python `venv`. And remember to install `beancount`.

```
$ python -m venv venv
$ . venv/bin/activate
(venv) $ pip install beancount
```

### Java 17 Requirement

- Please keep Java verion <= 17 for clj-python/libpython-clj to work properly. Java 21 would run into an issue.

## TODO

Remove Python dependency

## Developing in the container

This project currently relies on Python and OpenJDK. Since my local development environment is frequently affected by system package updates, I decided to create a Docker container to isolate the environment and ensure it only changes when absolutely needed.

One can start the REPL with
```
$ make docker-up
```
One can remove the container with
```
$ make docker-down
```

The container maps the timezone file and the `~/.m2` folder. The user root in container is `/app`, and project root in container is `/app/beancount-clj`.
The container exposes REPL at `0.0.0.0:3002` for interactive development and debugging.

